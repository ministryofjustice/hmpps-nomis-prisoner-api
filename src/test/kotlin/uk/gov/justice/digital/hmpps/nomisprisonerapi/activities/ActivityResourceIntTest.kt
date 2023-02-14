@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderProgramProfileBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderProgramProfilePayBandBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private const val PRISON_ID = "MDI"
private const val ROOM_ID: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val PROGRAM_CODE = "INTTEST"
private const val IEP_LEVEL = "STD"

class ActivityResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var courseActivityBuilderFactory: CourseActivityBuilderFactory

  @Autowired
  private lateinit var offenderProgramProfileBuilderFactory: OffenderProgramProfileBuilderFactory

  @Autowired
  private lateinit var offenderProgramProfilePayBandBuilderFactory: OffenderProgramProfilePayBandBuilderFactory

  @BeforeEach
  fun setup() {
    repository.save(ProgramServiceBuilder())
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
    repository.deleteActivities()
    repository.deleteProgramServices()
  }

  @Nested
  inner class CreateActivity {

    private val createActivityRequest: () -> CreateActivityRequest = {
      CreateActivityRequest(
        prisonId = PRISON_ID,
        code = "CA",
        programCode = PROGRAM_CODE,
        description = "test description",
        capacity = 23,
        startDate = LocalDate.parse("2022-10-31"),
        endDate = LocalDate.parse("2022-11-30"),
        minimumIncentiveLevelCode = IEP_LEVEL,
        internalLocationId = ROOM_ID,
        payRates = listOf(
          PayRateRequest(
            incentiveLevel = "BAS",
            payBand = "5",
            rate = BigDecimal(3.2),
          )
        ),
        payPerSession = "F",
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/activities")
        .body(BodyInserters.fromValue(createActivityRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createActivityRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createActivityRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access with prison not found`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createActivityRequest().copy(prisonId = "ZZX")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `will create activity with correct details`() {

      val id = callCreateEndpoint()

      // Spot check that the database has been populated.
      val courseActivity = repository.lookupActivity(id)

      assertThat(courseActivity.courseActivityId).isEqualTo(id)
      assertThat(courseActivity.capacity).isEqualTo(23)
      assertThat(courseActivity.prison.id).isEqualTo(PRISON_ID)
      assertThat(courseActivity.payRates.first().halfDayRate).isCloseTo(
        BigDecimal(0.4),
        within(BigDecimal("0.001"))
      )
      assertThat(courseActivity.payPerSession).isEqualTo(PayPerSession.F)
    }

    private fun callCreateEndpoint(): Long {
      val response = webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "prisonId" : "$PRISON_ID",
            "code" : "CA",
            "programCode" : "$PROGRAM_CODE",
            "description" : "test description",
            "capacity" : 23,
            "startDate" : "2022-10-31",
            "endDate" : "2022-11-30",
            "minimumIncentiveLevelCode" : "$IEP_LEVEL",
            "internalLocationId" : "$ROOM_ID",
            "payRates" : [ {
                "incentiveLevel" : "BAS",
                "payBand" : "5",
                "rate" : 0.4
                } ],
            "payPerSession": "F"    
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateActivityResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.courseActivityId).isGreaterThan(0)
      return response!!.courseActivityId
    }
  }

  @Nested
  inner class UpdateActivity {

    private lateinit var courseActivity: CourseActivity

    @BeforeEach
    fun setUp() {
      courseActivity = repository.save(courseActivityBuilderFactory.builder())
    }

    private fun updateActivityRequestJson(
      endDateJson: String? = """"endDate" : "2022-11-30",""",
      internalLocationJson: String? = """"internalLocationId" : -27,""",
      payRatesJson: String? = """
          "payRates" : [ {
              "incentiveLevel" : "STD",
              "payBand" : "5",
              "rate" : 0.8
              } ]
      """.trimIndent(),
    ) = """{
            ${endDateJson ?: ""}
            ${internalLocationJson ?: ""}
            ${payRatesJson ?: ""}
          }"""

    @Nested
    inner class Api {

      @Test
      fun `access forbidden when no authority`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(updateActivityRequestJson()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(updateActivityRequestJson()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(updateActivityRequestJson()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should update the activity and pay rate`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.internalLocation?.locationId).isEqualTo(-27)
        assertThat(updated.payRates[0].endDate).isEqualTo(LocalDate.now())
        assertThat(updated.payRates[1].endDate).isNull()
        assertThat(updated.payRates[1].halfDayRate)
          .isCloseTo(BigDecimal(0.8), within(BigDecimal(0.001)))
      }

      @Test
      fun `should return bad request for unknown data`() {
        val existingActivity =
          repository.activityRepository.findAll().firstOrNull() ?: throw BadDataException("No activities in database")

        callUpdateEndpoint(
          courseActivityId = existingActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(internalLocationJson = """"internalLocationId: -99999,"""),
        )
          .expectStatus().isBadRequest
      }

      @Test
      fun `should return not found for missing activity`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId() + 100,
          jsonBody = updateActivityRequestJson(),
        )
          .expectStatus().isNotFound
      }

      @Test
      fun `should return bad request for missing data`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(internalLocationJson = """"internalLocationId" : -27""", payRatesJson = null),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("payRates")
            assertThat(it).contains("NULL")
          }
      }

      @Test
      fun `should return bad request for malformed number`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(internalLocationJson = """"internalLocationId": "NOT_A_NUMBER","""),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("NOT_A_NUMBER")
          }
      }

      @Test
      fun `should return bad request for malformed date`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(endDateJson = """"endDate": "2022-11-35","""),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("2022-11-35")
          }
      }

      @Test
      fun `should return bad request for malformed number in child`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            payRatesJson = """
              "payRates" : [ {
                  "incentiveLevel" : "BAS",
                  "payBand" : "5",
                  "rate" : "NOT_A_NUMBER"
                  } ]
            """.trimIndent()
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("NOT_A_NUMBER")
          }
      }

      @Test
      fun `should return bad request for invalid pay band`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            payRatesJson = """
              "payRates" : [ {
                  "incentiveLevel" : "BAS",
                  "payBand" : "99",
                  "rate" : "1.2"
                  } ]
            """.trimIndent()
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Pay band code 99 does not exist")
          }
      }

      @Test
      fun `should return bad request when incentive level unavailable for prison`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            payRatesJson = """
              "payRates" : [ {
                  "incentiveLevel" : "EN2",
                  "payBand" : "5",
                  "rate" : "1.2"
                  } ]
            """.trimIndent()
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("IEP type EN2 does not exist for prison LEI")
          }
      }

      @Test
      fun `should return bad request for missing pay rates`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(payRatesJson = null, internalLocationJson = """"internalLocationId" : -27"""),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("payRates")
            assertThat(it).contains("NULL")
          }
      }

      @Test
      fun `should return OK for empty pay rates`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(payRatesJson = """ "payRates" : []"""),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.payRates[0].endDate).isEqualTo(LocalDate.now())
      }

      @Test
      fun `should return bad request if pay rate removed which is allocated to an offender`() {
        val offender =
          repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID)))
        repository.save(offenderProgramProfileBuilderFactory.builder(), offender.latestBooking(), courseActivity)

        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(payRatesJson = """ "payRates" : []"""),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Pay band 5 is allocated to offender(s) [A1234TT]")
          }
      }

      @Test
      fun `should return OK if pay rate removed which is NO LONGER allocated to an offender`() {
        val offender =
          repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID)))
        val existingActivityId = getSavedActivityId()
        repository.save(
          offenderProgramProfileBuilderFactory.builder(
            payBands = listOf(offenderProgramProfilePayBandBuilderFactory.builder(endDate = LocalDate.now().minusDays(1).toString()))
          ),
          offender.latestBooking(), courseActivity
        )

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(payRatesJson = """ "payRates" : []"""),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.payRates[0].endDate).isEqualTo(LocalDate.now())
      }
    }

    @Nested
    inner class ActivityDetails {
      @Test
      fun `should update details`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
        assertThat(updated.internalLocation?.locationId).isEqualTo(-27)
      }

      @Test
      fun `should allow nullable updates`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(endDateJson = null, internalLocationJson = null),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.scheduleEndDate).isNull()
        assertThat(updated.internalLocation).isNull()
      }
    }

    private fun callUpdateEndpoint(courseActivityId: Long, jsonBody: String) =
      webTestClient.put().uri("/activities/$courseActivityId")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonBody))
        .exchange()

    private fun getSavedActivityId() =
      repository.activityRepository.findAll().firstOrNull()?.courseActivityId
        ?: throw BadDataException("No activities in database")
  }

  // TODO SDI-610 temporary test to check new table and entity are OK - remove when we have proper integration tests
  @Nested
  inner class CourseSchedules {

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun `can save and load course schedules`() {
      val courseActivity = repository.save(courseActivityBuilderFactory.builder())
      entityManager.flush()

      val savedActivity = repository.lookupActivity(courseActivity.courseActivityId)

      with(savedActivity.courseSchedules[0]) {
        assertThat(courseScheduleId).isGreaterThan(0)
        assertThat(scheduleDate).isEqualTo(LocalDate.of(2022, 11, 1))
        assertThat(startTime).isEqualTo(LocalDateTime.of(2022, 11, 1, 8, 0))
        assertThat(endTime).isEqualTo(LocalDateTime.of(2022, 11, 1, 11, 0))
        assertThat(slotCategory).isEqualTo(SlotCategory.AM)
      }
    }
  }

  // TODO SDI-611 temporary test to check new table and entity are OK - remove when we have proper integration tests
  @Nested
  inner class CourseScheduleRules {

    @Test
    @Transactional
    fun `can save and load course schedule rules`() {
      val courseActivity = repository.save(courseActivityBuilderFactory.builder())

      val savedActivity = repository.lookupActivity(courseActivity.courseActivityId)

      with(savedActivity.courseScheduleRules[0]) {
        assertThat(id).isGreaterThan(0)
        assertThat(courseActivity.courseActivityId).isEqualTo(1)
        assertThat(monday).isTrue()
        assertThat(tuesday).isTrue()
        assertThat(wednesday).isTrue()
        assertThat(thursday).isTrue()
        assertThat(friday).isTrue()
        assertThat(saturday).isFalse()
        assertThat(sunday).isFalse()
        assertThat(startTime).isEqualTo(LocalDateTime.of(2022, 10, 31, 9, 30))
        assertThat(endTime).isEqualTo(LocalDateTime.of(2022, 10, 31, 12, 30))
        assertThat(slotCategory).isEqualTo(SlotCategory.AM)
      }
    }
  }
}
