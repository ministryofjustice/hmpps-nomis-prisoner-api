@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val prisonId = "MDI"
private const val roomId: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val programCode = "INTTEST"
private const val iepLevel = "STD"

private val createActivityRequest: () -> CreateActivityRequest = {
  CreateActivityRequest(
    prisonId = prisonId,
    code = "CA",
    programCode = programCode,
    description = "test description",
    capacity = 23,
    startDate = LocalDate.parse("2022-10-31"),
    endDate = LocalDate.parse("2022-11-30"),
    minimumIncentiveLevelCode = iepLevel,
    internalLocationId = roomId,
    payRates = listOf(
      PayRateRequest(
        incentiveLevel = "BAS",
        payBand = "5",
        rate = BigDecimal(3.2),
      )
    )
  )
}

class ActivitiesResourceIntTest : IntegrationTestBase() {

  private val TEN_DAYS_TIME = LocalDate.now().plusDays(10)

  @Autowired
  lateinit var repository: Repository

  @Autowired
  lateinit var courseActivityBuilderFactory: CourseActivityBuilderFactory

  lateinit var offenderAtMoorlands: Offender
  lateinit var offenderAtOtherPrison: Offender

  @BeforeEach
  internal fun setup() {
    repository.save(ProgramServiceBuilder())
    offenderAtMoorlands =
      repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = prisonId)))
    offenderAtOtherPrison =
      repository.save(OffenderBuilder(nomsId = "A1234XX").withBooking(OffenderBookingBuilder(agencyLocationId = "BXI")))
  }

  @AfterEach
  internal fun cleanUp() {
    repository.delete(offenderAtMoorlands)
    repository.delete(offenderAtOtherPrison)
    repository.deleteActivities()
    repository.deleteProgramServices()
  }

  @Nested
  inner class CreateActivity {

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
      assertThat(courseActivity.prison.id).isEqualTo(prisonId)
      assertThat(courseActivity.payRates.first().halfDayRate).isCloseTo(
        BigDecimal(0.4),
        within(BigDecimal("0.001"))
      )
    }

    private fun callCreateEndpoint(): Long {
      val response = webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "prisonId" : "$prisonId",
            "code" : "CA",
            "programCode" : "$programCode",
            "description" : "test description",
            "capacity" : 23,
            "startDate" : "2022-10-31",
            "endDate" : "2022-11-30",
            "minimumIncentiveLevelCode" : "$iepLevel",
            "internalLocationId" : "$roomId",
            "payRates" : [ {
                "incentiveLevel" : "BAS",
                "payBand" : "5",
                "rate" : 0.4
                } ]
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

    @BeforeEach
    fun setUp() {
      repository.save(courseActivityBuilderFactory.builder())
    }

    // Currently just mutates the location and pay rate - more to follow
    private fun updateActivityRequestJson(
      prisonIdJson: String? = """"prisonId": "LEI",""",
      capacityJson: String = """"capacity": 12,""",
      startDateJson: String = """"startDate" : "2022-10-31",""",
      internalLocationJson: String = """"internalLocationId" : -27,""",
      payRatesJson: String? = """
          "payRates" : [ {
              "incentiveLevel" : "BAS",
              "payBand" : "5",
              "rate" : 0.8
              } ]
      """.trimIndent(),
    ) = """{
            ${prisonIdJson ?: ""}
            "code" : "CA",
            "programCode" : "INTTEST",
            "description" : "test course activity",
            $capacityJson
            $startDateJson
            "endDate" : "2022-11-30",
            "minimumIncentiveLevelCode" : "STD",
            $internalLocationJson
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
        val existingActivity =
          repository.activityRepository.findAll().firstOrNull() ?: throw BadDataException("No activities in database")

        webTestClient.put().uri("/activities/${existingActivity.courseActivityId}")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(updateActivityRequestJson()))
          .exchange()
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivity.courseActivityId)
        assertAll(
          { assertThat(updated.internalLocation.locationId).isEqualTo(-27) },
          {
            assertThat(updated.payRates.first().halfDayRate).isEqualTo(
              BigDecimal(0.8).setScale(3, RoundingMode.HALF_UP)
            )
          }
        )
      }

      @Test
      fun `should return bad request for unknown data`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(updateActivityRequestJson(internalLocationJson = """"internalLocationId: -99999,""")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `should return not found for missing activity`() {
        webTestClient.put().uri("/activities/2")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(updateActivityRequestJson()))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `should return bad request for missing data`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(updateActivityRequestJson(prisonIdJson = null)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> { it.contains("prisonId") }
      }

      @Test
      fun `should return bad request for malformed number`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(updateActivityRequestJson(capacityJson = """"capacity": "NOT_A_NUMBER",""")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> { it.contains("capacity") }
      }

      @Test
      fun `should return bad request for malformed date`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(updateActivityRequestJson(startDateJson = """"startDate": "2022-11-35",""")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> { it.contains("startDate") }
      }

      @Test
      fun `should return bad request for malformed number in child`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              updateActivityRequestJson(
                payRatesJson = """
            "payRates" : [ {
                "incentiveLevel" : "BAS",
                "payBand" : "5",
                "rate" : 'NOT_A_NUMBER"
                } ]
                """.trimIndent()
              )
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> { it.contains("rate") }
      }

      @Test
      fun `should return bad request for missing pay rates`() {
        webTestClient.put().uri("/activities/1")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(updateActivityRequestJson(payRatesJson = null)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> { it.contains("payRates") }
      }

      @Test
      fun `should return OK for empty pay rates`() {
        val existingActivity =
          repository.activityRepository.findAll().firstOrNull() ?: throw BadDataException("No activities in database")

        webTestClient.put().uri("/activities/${existingActivity.courseActivityId}")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(updateActivityRequestJson(payRatesJson = """ "payRates" : []""")))
          .exchange()
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivity.courseActivityId)
        assertThat(updated.payRates).isEmpty()
      }
    }
  }

  @Nested
  inner class CreateOffenderProgramProfile {

    lateinit var courseActivity: CourseActivity

    private val createOffenderProgramProfileRequest: () -> CreateOffenderProgramProfileRequest = {
      CreateOffenderProgramProfileRequest(
        bookingId = offenderAtMoorlands.latestBooking().bookingId,
        startDate = LocalDate.parse("2022-11-14"),
        endDate = TEN_DAYS_TIME,
      )
    }

    @BeforeEach
    internal fun setup() {
      courseActivity = repository.save(courseActivityBuilderFactory.builder(prisonId = prisonId))
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access with activity not found`() {
      val response = webTestClient.post().uri("/activities/999888")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Not Found: Course activity with id=999888 does not exist")
    }

    @Test
    fun `access with booking not found`() {
      val response = webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest().copy(bookingId = 999888)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Bad request: Booking with id=999888 does not exist")
    }

    @Test
    fun `allocation already exists`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isCreated
      val response = webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Bad request: Offender Program Profile with courseActivityId=${courseActivity.courseActivityId} and bookingId=${offenderAtMoorlands.latestBooking().bookingId} already exists")
    }

    @Test
    fun `prisoner at different prison`() {
      val response = webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest().copy(bookingId = offenderAtOtherPrison.latestBooking().bookingId)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Bad request: Prisoner is at prison=BXI, not the Course activity prison=$prisonId")
    }

    @Test
    fun `activity expired`() {
      val expired = repository.save(courseActivityBuilderFactory.builder(prisonId = prisonId, endDate = "2022-12-14"))
      val response = webTestClient.post().uri("/activities/${expired.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Bad request: Course activity with id=${expired.courseActivityId} has expired")
    }

    @Test
    fun `start date missing`() {
      val response = webTestClient.post().uri("/activities/4567")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "bookingId" : "1234",
            "endDate"   : "2022-11-30"
          }"""
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).contains("value failed for JSON property startDate due to missing (therefore NULL) value")
    }

    @Test
    fun `will create profile with correct details`() {

      val bookingId = repository.lookupOffender("A1234TT")?.latestBooking()?.bookingId!!
      val id = callCreateEndpoint(courseActivity.courseActivityId, bookingId)

      // Spot check that the database has been populated correctly.
      val persistedRecord = repository.lookupOffenderProgramProfile(id)
      assertThat(persistedRecord.courseActivity?.courseActivityId).isEqualTo(courseActivity.courseActivityId)
      with(persistedRecord) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(program.programCode).isEqualTo(programCode)
        assertThat(startDate).isEqualTo(LocalDate.parse("2022-11-14"))
        assertThat(endDate).isEqualTo(TEN_DAYS_TIME)
      }
    }

    private fun callCreateEndpoint(courseActivityId: Long, bookingId: Long): Long {
      val response = webTestClient.post().uri("/activities/$courseActivityId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "bookingId" : "$bookingId",
            "startDate" : "2022-11-14",
            "endDate"   : "${TEN_DAYS_TIME.format(DateTimeFormatter.ISO_DATE)}"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateOffenderProgramProfileResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      return response!!.offenderProgramReferenceId
    }
  }
}
