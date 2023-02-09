@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityPayRateBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderProgramProfileBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderProgramProfilePayBandBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import java.math.BigDecimal
import java.time.LocalDate

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
  private lateinit var courseActivityPayRateBuilderFactory: CourseActivityPayRateBuilderFactory

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

    @Nested
    inner class PayRates {
      @Test
      fun `no change should do nothing`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpointIsOk(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(
            internalLocationJson = """"internalLocationId" : -8,""",
            payRatesJson = """ "payRates" : [ { "incentiveLevel" : "STD", "payBand" : "5", "rate" : 3.2 } ] """,
          ),
        )

        val updatedRates = repository.lookupActivity(existingActivityId).payRates
        assertThat(updatedRates.size).isEqualTo(1)
        with(updatedRates.first()) {
          assertThat(id.iepLevelCode).isEqualTo("STD")
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        }
      }

      @Test
      fun `adding should create new pay rate effective today`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpointIsOk(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(
            payRatesJson = """ "payRates" : [ 
              { "incentiveLevel" : "STD", "payBand" : "5", "rate" : 3.2 }, 
              { "incentiveLevel" : "STD", "payBand" : "6", "rate" : 3.4 } 
              ] """.trimMargin(),
          ),
        )

        val updatedRates = repository.lookupActivity(existingActivityId).payRates
        assertThat(updatedRates.size).isEqualTo(2)
        // existing rate unchanged
        with(updatedRates[0]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isNull()
        }
        // new rate added
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("6")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.4), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now())
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `amending should expire existing and create new pay rate effective tomorrow`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpointIsOk(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(
            payRatesJson = """ "payRates" : [ 
              { "incentiveLevel" : "STD", "payBand" : "5", "rate" : 4.3 }
              ] """.trimMargin(),
          ),
        )

        val updatedRates = repository.lookupActivity(existingActivityId).payRates
        assertThat(updatedRates.size).isEqualTo(2)
        // old rate has been expired
        with(updatedRates[0]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
        // new rate created from tomorrow
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(4.3), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now().plusDays(1))
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `re-adding previously expired rate should add new rate effective today`() {
        val existingActivity = repository.save(
          courseActivityBuilderFactory.builder(
            payRates = listOf(
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "5",
                startDate = LocalDate.now().minusDays(10).toString(),
                endDate = LocalDate.now().minusDays(3).toString(),
                halfDayRate = BigDecimal(3.2)
              ),
            )
          )
        )
        callUpdateEndpointIsOk(
          courseActivityId = existingActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(
            payRatesJson = """ "payRates" : [ 
              { "incentiveLevel" : "STD", "payBand" : "5", "rate" : 4.3 }
              ] """.trimMargin(),
          ),
        )

        val updatedRates = repository.lookupActivity(existingActivity.courseActivityId).payRates
        assertThat(updatedRates.size).isEqualTo(2)
        // old rate not changed
        with(updatedRates[0]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now().minusDays(3))
        }
        // new rate created from today
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(4.3), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now())
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `amending new rate starting tomorrow should not cause an expiry (adjusting rate twice in one day)`() {
        val existingActivity = repository.save(
          courseActivityBuilderFactory.builder(
            payRates = listOf(
              // old rate expires today
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "5",
                startDate = LocalDate.now().minusDays(1).toString(),
                endDate = LocalDate.now().toString(),
                halfDayRate = BigDecimal(3.2)
              ),
              // new rate becomes effective tomorrow
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "5",
                startDate = LocalDate.now().plusDays(1).toString(),
                halfDayRate = BigDecimal(4.3)
              ),
            )
          )
        )

        callUpdateEndpointIsOk(
          courseActivityId = existingActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(
            payRatesJson = """ "payRates" : [ 
              { "incentiveLevel" : "STD", "payBand" : "5", "rate" : 5.4 }
              ] """.trimMargin(),
          ),
        )

        val updatedRates = repository.lookupActivity(existingActivity.courseActivityId).payRates
        assertThat(updatedRates.size).isEqualTo(2)
        // old rate still expired
        with(updatedRates[0]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
        // new rate with adjusted half day rate is still effective from tomorrow
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(5.4), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now().plusDays(1))
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `removing new rate starting tomorrow should delete the rate rather than expire`() {
        val existingActivity = repository.save(
          courseActivityBuilderFactory.builder(
            payRates = listOf(
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "5",
                startDate = LocalDate.now().minusDays(1).toString(),
                endDate = LocalDate.now().toString(),
                halfDayRate = BigDecimal(3.2)
              ),
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "5",
                startDate = LocalDate.now().plusDays(1).toString(),
                halfDayRate = BigDecimal(4.3)
              ),
            )
          )
        )

        callUpdateEndpointIsOk(
          courseActivityId = existingActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(
            payRatesJson = """ "payRates" : [] """.trimMargin(),
          ),
        )

        val updatedRates = repository.lookupActivity(existingActivity.courseActivityId).payRates
        // We only have the old expired rate - the future rate is now removed
        assertThat(updatedRates.size).isEqualTo(1)
        // old rate still expired
        with(updatedRates[0]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
      }

      @Test
      fun `missing rate should be expired`() {
        val existingActivityId = getSavedActivityId()

        // request pay band 6 instead of 5
        callUpdateEndpointIsOk(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(
            payRatesJson = """ "payRates" : [ 
              { "incentiveLevel" : "STD", "payBand" : "6", "rate" : 6.5 }
              ] """.trimMargin(),
          ),
        )

        val updatedRates = repository.lookupActivity(existingActivityId).payRates
        assertThat(updatedRates.size).isEqualTo(2)
        // missing rate for pay band 5 has been expired
        with(updatedRates[0]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
        // new rate for pay band 6 effective from today
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("6")
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(6.5), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now())
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `should be able to add, amend, amend future rate and delete rate at the same time`() {
        val existingActivity = repository.save(
          courseActivityBuilderFactory.builder(
            payRates = listOf(
              // rate is expired
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "5",
                startDate = LocalDate.now().minusDays(1).toString(),
                endDate = LocalDate.now().toString(),
                halfDayRate = BigDecimal(3.2)
              ),
              // new rate active from tomorrow
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "5",
                startDate = LocalDate.now().plusDays(1).toString(),
                halfDayRate = BigDecimal(4.3)
              ),
              // rate expires today
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "6",
                startDate = LocalDate.now().minusDays(1).toString(),
                endDate = LocalDate.now().toString(),
                halfDayRate = BigDecimal(5.3)
              ),
              // rate currently active
              courseActivityPayRateBuilderFactory.builder(
                iepLevelCode = "STD",
                payBandCode = "7",
                startDate = LocalDate.now().minusDays(1).toString(),
                halfDayRate = BigDecimal(8.7)
              ),
            )
          )
        )

        callUpdateEndpointIsOk(
          courseActivityId = existingActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(
            payRatesJson = """ "payRates" : [ 
            { "incentiveLevel" : "STD", "payBand" : "5", "rate" : 4.4 }, 
            { "incentiveLevel" : "STD", "payBand" : "6", "rate" : 5.4 }
            ] """.trimMargin(),
          ),
        )

        val updatedRates = repository.lookupActivity(existingActivity.courseActivityId).payRates
        assertThat(updatedRates.size).isEqualTo(5)
        // old rate for pay band 5 is still expired
        with(updatedRates.findRate("STD", "5", expired = true)) {
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        }
        // new rate for pay band 5 has been updated
        with(updatedRates.findRate("STD", "5", expired = false)) {
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(4.4), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now().plusDays(1))
        }
        // old rate for pay band 6 is still expired
        with(updatedRates.findRate("STD", "6", expired = true)) {
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(5.3), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
        // new rate for pay band 6 applicable from tomorrow
        with(updatedRates.findRate("STD", "6", expired = false)) {
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(5.4), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now().plusDays(1))
        }
        // old rate for pay band 7 has been expired
        with(updatedRates.findRate("STD", "7", expired = true)) {
          assertThat(halfDayRate)
            .isCloseTo(BigDecimal(8.7), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
      }
    }

    private fun callUpdateEndpoint(courseActivityId: Long, jsonBody: String) =
      webTestClient.put().uri("/activities/$courseActivityId")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonBody))
        .exchange()

    private fun callUpdateEndpointIsOk(courseActivityId: Long, jsonBody: String) =
      callUpdateEndpoint(courseActivityId, jsonBody)
        .expectStatus().isOk

    private fun getSavedActivityId() =
      repository.activityRepository.findAll().firstOrNull()?.courseActivityId
        ?: throw BadDataException("No activities in database")

    private fun MutableList<CourseActivityPayRate>.findRate(
      iepLevelCode: String,
      payBandCode: String,
      expired: Boolean
    ): CourseActivityPayRate =
      firstOrNull { it.id.iepLevelCode == iepLevelCode && it.id.payBandCode == payBandCode && if (expired) it.endDate != null else it.endDate == null }!!
  }
}
