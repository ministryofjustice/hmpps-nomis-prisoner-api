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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityPayRateBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.math.BigDecimal
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

  @Autowired
  lateinit var courseActivityPayRateBuilderFactory: CourseActivityPayRateBuilderFactory

  lateinit var offenderAtMoorlands: Offender
  lateinit var offenderAtOtherPrison: Offender

  private fun callCreateEndpoint(courseActivityId: Long, bookingId: Long): Long {
    val response = webTestClient.post().uri("/activities/$courseActivityId")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          """{
            "bookingId"  : "$bookingId",
            "startDate"  : "2022-11-14",
            "endDate"    : "${TEN_DAYS_TIME.format(DateTimeFormatter.ISO_DATE)}",
            "payBandCode": "5"
          }"""
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody(OffenderProgramProfileResponse::class.java)
      .returnResult().responseBody
    assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
    return response!!.offenderProgramReferenceId
  }

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
              "incentiveLevel" : "STD",
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
        val existingActivityId = getSavedActivityId()

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.internalLocation.locationId).isEqualTo(-27)
        assertThat(updated.payRates[0].endDate).isEqualTo(LocalDate.now())
        assertThat(updated.payRates[1].endDate).isNull()
        assertThat(updated.payRates[1].halfDayRate).isCloseTo(BigDecimal(0.8), within(BigDecimal(0.001)))
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
          jsonBody = updateActivityRequestJson(prisonIdJson = null),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("prisonId")
            assertThat(it).contains("NULL")
          }
      }

      @Test
      fun `should return bad request for malformed number`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(capacityJson = """"capacity": "NOT_A_NUMBER","""),
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
          jsonBody = updateActivityRequestJson(startDateJson = """"startDate": "2022-11-35","""),
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
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
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
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isNull()
        }
        // new rate added
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("6")
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.4), within(BigDecimal(0.001)))
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
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
        // new rate created from tomorrow
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate).isCloseTo(BigDecimal(4.3), within(BigDecimal(0.001)))
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
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now().minusDays(3))
        }
        // new rate created from today
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate).isCloseTo(BigDecimal(4.3), within(BigDecimal(0.001)))
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
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
        // new rate with adjusted half day rate is still effective from tomorrow
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("5")
          assertThat(halfDayRate).isCloseTo(BigDecimal(5.4), within(BigDecimal(0.001)))
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
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
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
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
        // new rate for pay band 6 effective from today
        with(updatedRates[1]) {
          assertThat(id.payBandCode).isEqualTo("6")
          assertThat(halfDayRate).isCloseTo(BigDecimal(6.5), within(BigDecimal(0.001)))
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
          assertThat(halfDayRate).isCloseTo(BigDecimal(3.2), within(BigDecimal(0.001)))
        }
        // new rate for pay band 5 has been updated
        with(updatedRates.findRate("STD", "5", expired = false)) {
          assertThat(halfDayRate).isCloseTo(BigDecimal(4.4), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now().plusDays(1))
        }
        // old rate for pay band 6 is still expired
        with(updatedRates.findRate("STD", "6", expired = true)) {
          assertThat(halfDayRate).isCloseTo(BigDecimal(5.3), within(BigDecimal(0.001)))
          assertThat(endDate).isEqualTo(LocalDate.now())
        }
        // new rate for pay band 6 applicable from tomorrow
        with(updatedRates.findRate("STD", "6", expired = false)) {
          assertThat(halfDayRate).isCloseTo(BigDecimal(5.4), within(BigDecimal(0.001)))
          assertThat(id.startDate).isEqualTo(LocalDate.now().plusDays(1))
        }
        // old rate for pay band 7 has been expired
        with(updatedRates.findRate("STD", "7", expired = true)) {
          assertThat(halfDayRate).isCloseTo(BigDecimal(8.7), within(BigDecimal(0.001)))
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

  @Nested
  inner class CreateOffenderProgramProfile {

    lateinit var courseActivity: CourseActivity

    private val createOffenderProgramProfileRequest: () -> CreateOffenderProgramProfileRequest = {
      CreateOffenderProgramProfileRequest(
        bookingId = offenderAtMoorlands.latestBooking().bookingId,
        startDate = LocalDate.parse("2022-11-14"),
        endDate = TEN_DAYS_TIME,
        payBandCode = "5",
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

      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      val id = callCreateEndpoint(courseActivity.courseActivityId, bookingId)

      // Spot check that the database has been populated correctly.
      val persistedRecord = repository.lookupOffenderProgramProfile(id)
      assertThat(persistedRecord.courseActivity?.courseActivityId).isEqualTo(courseActivity.courseActivityId)
      with(persistedRecord) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(program.programCode).isEqualTo(programCode)
        assertThat(startDate).isEqualTo(LocalDate.parse("2022-11-14"))
        assertThat(endDate).isEqualTo(TEN_DAYS_TIME)
        val payBand = payBands.first()
        assertThat(payBand.offenderProgramProfile.offenderProgramReferenceId).isEqualTo(offenderProgramReferenceId)
        assertThat(payBand.startDate).isEqualTo(startDate)
        assertThat(payBand.endDate).isEqualTo(endDate)
        assertThat(payBand.payBand.code).isEqualTo("5")
      }
    }
  }

  @Nested
  inner class EndOffenderProgramProfile {

    lateinit var courseActivity: CourseActivity
    var bookingId: Long = 0

    private val endOffenderProgramProfileRequest: () -> EndOffenderProgramProfileRequest = {
      EndOffenderProgramProfileRequest(
        endDate = LocalDate.parse("2023-01-28"),
        endReason = "REL",
        endComment = "A comment",
      )
    }

    @BeforeEach
    internal fun setup() {
      courseActivity = repository.save(courseActivityBuilderFactory.builder(prisonId = prisonId))
      bookingId = offenderAtMoorlands.latestBooking().bookingId
      callCreateEndpoint(courseActivity.courseActivityId, bookingId)
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `course activity not found`() {
      webTestClient.put().uri("/activities/999888/booking-id/$bookingId/end")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Not Found: Course activity with id=999888 does not exist")
    }

    @Test
    fun `booking not found`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/999888/end")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Not Found: Booking with id=999888 does not exist")
    }

    @Test
    fun `the prisoner is not allocated to the course`() {
      val otherBookingId = offenderAtOtherPrison.latestBooking().bookingId

      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$otherBookingId/end")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo("Bad request: Offender Program Profile with courseActivityId=${courseActivity.courseActivityId} and bookingId=$otherBookingId and status=ALLOC does not exist")
    }

    @Test
    fun `end date missing`() {
      val response =
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "endReason" : "REL"
          }"""
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody
      assertThat(response?.userMessage).contains("value failed for JSON property endDate due to missing (therefore NULL) value")
    }

    @Test
    fun `end date invalid`() {
      val response =
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "endDate" : "invalid"
          }"""
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody
      assertThat(response?.userMessage).contains("Text 'invalid' could not be parsed at index 0")
    }

    @Test
    fun `text too long`() {
      val response =
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(endOffenderProgramProfileRequest().copy(endReason = "ThirteenChars")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody
      assertThat(response?.userMessage).contains("length must be between 0 and 12")
    }

    @Test
    fun `the end reason is invalid`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest().copy(endReason = "DUFF")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Bad request: End reason code=DUFF is invalid")
    }

    @Test
    fun `will create profile with correct details`() {

      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      val id = callEndpoint(courseActivity.courseActivityId, bookingId)

      // Spot check that the database has been populated correctly.
      val persistedRecord = repository.lookupOffenderProgramProfile(id)
      assertThat(persistedRecord.courseActivity?.courseActivityId).isEqualTo(courseActivity.courseActivityId)
      with(persistedRecord) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(endDate).isEqualTo(LocalDate.parse("2023-01-28"))
        assertThat(endReason?.code).isEqualTo("REL")
        assertThat(endComment).isEqualTo("A comment")
      }
    }

    private fun callEndpoint(courseActivityId: Long, bookingId: Long): Long {
      val response =
        webTestClient.put().uri("/activities/$courseActivityId/booking-id/$bookingId/end")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "endDate"   : "2023-01-28",
            "endReason" : "REL",
            "endComment": "A comment"
          }"""
            )
          )
          .exchange()
          .expectStatus().isOk
          .expectBody(OffenderProgramProfileResponse::class.java)
          .returnResult().responseBody
      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      return response!!.offenderProgramReferenceId
    }
  }
}
