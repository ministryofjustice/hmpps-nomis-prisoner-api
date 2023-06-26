@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.ActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.CourseActivityAreaRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseScheduleBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.IncentiveBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderCourseAttendanceBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderProgramProfileBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderProgramProfilePayBandBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.testData
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityArea
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private const val PRISON_ID = "LEI"
private const val ROOM_ID: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val PROGRAM_CODE = "INTTEST"
private const val IEP_LEVEL = "STD"

class ActivityResourceIntTest : IntegrationTestBase() {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var courseActivityBuilderFactory: CourseActivityBuilderFactory

  @Autowired
  private lateinit var offenderProgramProfileBuilderFactory: OffenderProgramProfileBuilderFactory

  @Autowired
  private lateinit var offenderProgramProfilePayBandBuilderFactory: OffenderProgramProfilePayBandBuilderFactory

  @Autowired
  private lateinit var offenderCourseAttendanceBuilderFactory: OffenderCourseAttendanceBuilderFactory

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  @BeforeEach
  fun setup() {
    testData(repository) {
      programService {}
    }
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
    repository.deleteActivities()
    repository.deleteProgramServices()
  }

  @Nested
  inner class CreateActivity {

    @Nested
    inner class Api {
      @Test
      fun `access forbidden when no authority`() {
        webTestClient.post().uri("/activities")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/activities")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/activities")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `invalid prison should return bad request`() {
        val invalidPrison = validJsonRequest().replace(""""prisonId" : "$PRISON_ID",""", """"prisonId" : "ZZX",""")

        createActivityExpectingBadRequest(invalidPrison)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Prison with id=ZZX does not exist")
          }
      }

      @Test
      fun `Invalid activity code should return bad request`() {
        val invalidSchedule = validJsonRequest().replace(""""code" : "CA",""", """"code" : "1234567890123",""")

        createActivityExpectingBadRequest(invalidSchedule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("code").contains("1234567890123").contains("length must be between 1 and 12")
          }
      }

      @Test
      fun `Invalid capacity should return bad request`() {
        val invalidSchedule = validJsonRequest().replace(""""capacity" : 23,""", """"capacity" : 1000,""")

        createActivityExpectingBadRequest(invalidSchedule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("capacity").contains("1000").contains("must be less than or equal to 999")
          }
      }

      @Test
      fun `Invalid description should return bad request`() {
        val invalidSchedule = validJsonRequest().replace(""""description" : "test description",""", """"description" : "12345678901234567890123456789012345678901",""")

        createActivityExpectingBadRequest(invalidSchedule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("description").contains("length must be between 1 and 40")
          }
      }
    }

    @Nested
    inner class Schedules {
      @Test
      fun `invalid schedule start time should return bad request`() {
        val invalidSchedule = validJsonRequest().replace(""""startTime": "11:45"""", """"startTime": "11:65",""")

        createActivityExpectingBadRequest(invalidSchedule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("11:65")
          }
      }

      @Test
      fun `invalid schedule end time should return bad request`() {
        val invalidSchedule = validJsonRequest().replace(""""endTime": "12:35"""", """"endTime": "12:65"""")

        createActivityExpectingBadRequest(invalidSchedule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("12:65")
          }
      }

      @Test
      fun `invalid schedule date should return bad request`() {
        val invalidSchedule = validJsonRequest().replace(""""date": "2022-10-31",""", """"date": "2022-13-31",""")

        createActivityExpectingBadRequest(invalidSchedule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("2022-13-31")
          }
      }
    }

    @Nested
    inner class ScheduleRules {
      @Test
      fun `invalid schedule rule time should return bad request`() {
        val invalidScheduleRule = validJsonRequest().replace(""""startTime": "11:45"""", """"startTime": "11:61"""")

        createActivityExpectingBadRequest(invalidScheduleRule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Invalid value for MinuteOfHour (valid values 0 - 59): 61")
          }
      }

      @Test
      fun `Invalid schedule rule day of week should return bad request`() {
        val invalidScheduleRule = validJsonRequest().replace(""""monday": false,""", """"monday": "INVALID",""")

        createActivityExpectingBadRequest(invalidScheduleRule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("INVALID")
          }
      }
    }

    @Nested
    inner class PayRates {
      @Test
      fun `error from pay rate service should return bad request`() {
        val invalidSchedule = validJsonRequest().replace(""""payBand" : "5",""", """"payBand" : "INVALID",""")

        createActivityExpectingBadRequest(invalidSchedule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Pay band code INVALID does not exist")
          }
      }

      @Test
      fun `Invalid pay per session should return bad request`() {
        val invalidSchedule = validJsonRequest().replace(""""payPerSession": "F",""", """"payPerSession": "f",""")

        createActivityExpectingBadRequest(invalidSchedule)
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("PayPerSession").contains("""String "f": not one of the values accepted for Enum class: [H, F]""")
          }
      }
    }

    @Nested
    inner class ActivityDetails {
      @Test
      fun `will create activity with correct details`() {
        val monthStart = LocalDate.now().withDayOfMonth(1).toString()
        val id = callCreateEndpoint()

        // Spot check that the database has been populated.
        val courseActivity = repository.lookupActivity(id)

        assertThat(courseActivity.courseActivityId).isEqualTo(id)
        assertThat(courseActivity.capacity).isEqualTo(23)
        assertThat(courseActivity.prison.id).isEqualTo(PRISON_ID)
        assertThat(courseActivity.payRates.first().halfDayRate).isCloseTo(
          BigDecimal(0.4),
          within(BigDecimal("0.001")),
        )
        assertThat(courseActivity.payPerSession).isEqualTo(PayPerSession.F)
        assertThat(courseActivity.courseSchedules.first().scheduleDate).isEqualTo("2022-10-31")
        with(courseActivity.courseScheduleRules.first()) {
          assertThat(id).isGreaterThan(0)
          assertThat(this.courseActivity.courseActivityId).isEqualTo(courseActivity.courseActivityId)
          assertThat(monday).isFalse
          assertThat(tuesday).isTrue
          assertThat(wednesday).isFalse
          assertThat(thursday).isTrue
          assertThat(friday).isTrue
          assertThat(saturday).isFalse
          assertThat(sunday).isFalse
          assertThat(startTime).isEqualTo(LocalDateTime.parse("${monthStart}T11:45:00"))
          assertThat(endTime).isEqualTo(LocalDateTime.parse("${monthStart}T12:35:00"))
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
        assertThat(courseActivity.excludeBankHolidays).isEqualTo(true)
      }

      @Test
      fun `should raise telemetry event`() {
        val id = callCreateEndpoint()
        val courseActivity = repository.lookupActivity(id)

        verify(telemetryClient).trackEvent(
          eq("activity-created"),
          check<MutableMap<String, String>> { actual ->
            mapOf(
              "nomisCourseActivityId" to id.toString(),
              "prisonId" to PRISON_ID,
              "nomisCourseScheduleIds" to "[${courseActivity.courseSchedules[0].courseScheduleId}]",
              "nomisCourseActivityPayRateIds" to "[BAS-5-2022-10-31]",
              "nomisCourseScheduleRuleIds" to "[${courseActivity.courseScheduleRules[0].id}]",
            ).also { expected ->
              log.info("expected telemetry details to be: $expected")
              assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected)
            }
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class Response {
      @Test
      fun `will respond with all course schedule details`() {
        val request = validJsonRequest(
          """
              "schedules" : [ 
                {
                  "date": "2022-10-31",
                  "startTime" : "09:00",
                  "endTime" : "11:00"
                },
                {
                  "date": "2022-11-01",
                  "startTime" : "13:00",
                  "endTime" : "15:00"
                }
              ],
          """.trimIndent(),
        )
        callCreateEndpointAndExpect(request)
          .expectBody()
          .jsonPath("courseActivityId").value<Int> { assertThat(it).isGreaterThan(0) }
          .jsonPath("courseSchedules[0].courseScheduleId").value<Int> { assertThat(it).isGreaterThan(0) }
          .jsonPath("courseSchedules[0].date").isEqualTo("2022-10-31")
          .jsonPath("courseSchedules[0].startTime").isEqualTo("09:00:00")
          .jsonPath("courseSchedules[0].endTime").isEqualTo("11:00:00")
          .jsonPath("courseSchedules[1].courseScheduleId").value<Int> { assertThat(it).isGreaterThan(0) }
          .jsonPath("courseSchedules[1].date").isEqualTo("2022-11-01")
          .jsonPath("courseSchedules[1].startTime").isEqualTo("13:00:00")
          .jsonPath("courseSchedules[1].endTime").isEqualTo("15:00:00")
      }
    }

    private fun createActivityExpectingBadRequest(body: String) =
      webTestClient.post().uri("/activities")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(body))
        .exchange()
        .expectStatus().isBadRequest

    private fun callCreateEndpoint(): Long {
      val response = callCreateEndpointAndExpect()
        .expectBody(ActivityResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.courseActivityId).isGreaterThan(0)
      return response!!.courseActivityId
    }

    private fun callCreateEndpointAndExpect(request: String = validJsonRequest()) =
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(request))
        .exchange()
        .expectStatus().isCreated

    private fun validJsonRequest(
      schedulesJson: String = """
              "schedules" : [ {
                  "date": "2022-10-31",
                  "startTime" : "09:00",
                  "endTime" : "11:00"
              } ],
      """.trimIndent(),
    ) = """{
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
            "payPerSession": "F",
            $schedulesJson
            "scheduleRules": [{
              "startTime": "11:45",
              "endTime": "12:35",
              "monday": false,
              "tuesday": true,
              "wednesday": false,
              "thursday": true,
              "friday": true,
              "saturday": false,
              "sunday": false
              }],
            "excludeBankHolidays": true
          }
    """.trimIndent()
  }

  @Nested
  inner class UpdateActivity {

    private lateinit var courseActivity: CourseActivity

    @BeforeEach
    fun setUp() {
      testData(repository) {
        programService {
          courseActivity = courseActivity { payRate() }
        }
      }
    }

    private fun detailsJson(): String = """
      "startDate" : "2022-11-01",
      "endDate" : "2022-11-30",
      "internalLocationId" : -27,
      "capacity": 30,
      "description": "updated description", 
      "minimumIncentiveLevelCode": "BAS", 
      "payPerSession": "F", 
      "excludeBankHolidays": true, 
      "programCode": "INTTEST",
    """.trimIndent()

    private fun payRatesJson(): String = """
      "payRates" : [ {
          "incentiveLevel" : "STD",
          "payBand" : "5",
          "rate" : 0.8
          } ],
    """.trimIndent()

    private fun scheduleRulesJson(): String = """
      "scheduleRules": [{
          "startTime": "09:30",
          "endTime": "12:30",
          "monday": true,
          "tuesday": true,
          "wednesday": true,
          "thursday": true,
          "friday": true,
          "saturday": false,
          "sunday": false
          }],
    """.trimIndent()

    private fun schedulesJson() = """
      "schedules": [{
          "id": "${courseActivity.courseSchedules[0].courseScheduleId}",
          "date": "2022-11-01",
          "startTime": "08:00",
          "endTime": "11:00"
        }]
    """.trimIndent()

    private fun updateActivityRequestJson(
      detailsJson: String = detailsJson(),
      payRatesJson: String? = payRatesJson(),
      scheduleRulesJson: String? = scheduleRulesJson(),
      schedulesJson: String? = schedulesJson(),
    ) = """{
            $detailsJson
            ${payRatesJson ?: ""}
            ${scheduleRulesJson ?: ""}
            ${schedulesJson ?: ""}
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
      fun `should raise telemetry event`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(),
        )
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("activity-updated"),
          check<MutableMap<String, String>> {
            assertThat(it["nomisCourseActivityId"]).isEqualTo(existingActivityId.toString())
            assertThat(it["prisonId"]).isEqualTo("LEI")
            assertThat(it["created-courseActivityPayRateIds"]).isEqualTo("[STD-5-$tomorrow]")
            assertThat(it["expired-courseActivityPayRateIds"]).isEqualTo("[STD-5-2022-10-31]")
          },
          isNull(),
        )
      }

      @Test
      fun `should return bad request for unknown data`() {
        val existingActivity =
          repository.activityRepository.findAll().firstOrNull() ?: throw BadDataException("No activities in database")

        callUpdateEndpoint(
          courseActivityId = existingActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(detailsJson = detailsJson().replace(""""internalLocationId" : -27,""", """"internalLocationId: -99999,""")),
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
      fun `should return bad request for malformed number`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(detailsJson = detailsJson().replace(""""internalLocationId" : -27,""", """"internalLocationId": "NOT_A_NUMBER",""")),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("NOT_A_NUMBER")
          }
      }

      @Test
      fun `should return bad request for malformed start date`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(detailsJson = detailsJson().replace(""""startDate" : "2022-11-01",""", """"startDate": "2021-13-01",""")),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("2021-13-01")
          }
      }

      @Test
      fun `should return bad request for malformed end date`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(detailsJson = detailsJson().replace(""""endDate" : "2022-11-30",""", """"endDate": "2022-11-35",""")),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("2022-11-35")
          }
      }

      @Test
      fun `should return bad request for dates out of order`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(detailsJson = detailsJson().replace(""""endDate" : "2022-11-30",""", """"endDate": "2022-10-31",""")),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Start date 2022-11-01 must not be after end date 2022-10-31")
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
                  } ],
            """.trimIndent(),
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("NOT_A_NUMBER")
          }
      }
    }

    @Nested
    inner class PayRates {

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
                  } ],
            """.trimIndent(),
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
                  } ],
            """.trimIndent(),
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
          jsonBody = updateActivityRequestJson(payRatesJson = null),
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
          jsonBody = updateActivityRequestJson(payRatesJson = """ "payRates" : [],"""),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.payRates[0].endDate).isEqualTo(LocalDate.now())
      }

      @Test
      fun `should return bad request if pay rate removed which is allocated to an offender`() {
        testData(repository) {
          programService {
            courseActivity = courseActivity {
              payRate(iepLevelCode = "STD")
              payRate(iepLevelCode = "BAS")
            }
          }
// TODO SDIT-902
//          offender = offender(nomsId = "A1234TT") {
//            offenderBooking = booking(agencyLocationId = PRISON_ID) {
//              incentive()
//            }
//          }
//          allocation(offenderBooking = offenderBooking, courseActivity = courseActivity)
        }
        val offenderBooking = OffenderBookingBuilder(agencyLocationId = PRISON_ID, incentives = listOf(IncentiveBuilder(iepLevel = "STD")))
        val offender = repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(offenderBooking))
        repository.save(offenderProgramProfileBuilderFactory.builder(), offender.latestBooking(), courseActivity)

        val payRatesJson = """
          "payRates" : [ {
            "incentiveLevel" : "BAS",
            "payBand" : "5",
            "rate" : 0.8
          } ],
        """.trimIndent()
        callUpdateEndpoint(
          courseActivityId = courseActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(payRatesJson = payRatesJson), // remove the STD pay rate that is being used by the offender
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Pay band 5 for incentive level STD is allocated to offender(s) [A1234TT]")
          }
      }

      // This might seem an odd test but it replicates a bug found here https://dsdmoj.atlassian.net/browse/SDIT-846
      @Test
      fun `should return OK if unused pay rate removed with a pay band used on a different pay rate`() {
        testData(repository) {
          programService {
            courseActivity = courseActivity {
              payRate(iepLevelCode = "STD")
              payRate(iepLevelCode = "BAS")
            }
          }
        }
        val offenderBooking = OffenderBookingBuilder(agencyLocationId = PRISON_ID, incentives = listOf(IncentiveBuilder(iepLevel = "STD")))
        val offender = repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(offenderBooking))
        repository.save(offenderProgramProfileBuilderFactory.builder(), offender.latestBooking(), courseActivity)

        callUpdateEndpoint(
          courseActivityId = courseActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(), // remove the BAS pay rate that is not being used
        )
          .expectStatus().isOk
      }

      @Test
      fun `should return OK if pay rate removed which is NO LONGER allocated to an offender`() {
        val offender =
          repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID, incentives = listOf(IncentiveBuilder(iepLevel = "STD")))))
        val existingActivityId = getSavedActivityId()
        repository.save(
          offenderProgramProfileBuilderFactory.builder(
            payBands = listOf(offenderProgramProfilePayBandBuilderFactory.builder(endDate = LocalDate.now().minusDays(1).toString())),
          ),
          offender.latestBooking(),
          courseActivity,
        )

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(payRatesJson = """ "payRates" : [],"""),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.payRates[0].endDate).isEqualTo(LocalDate.now())
      }
    }

    @Nested
    inner class ScheduleRules {

      @Test
      fun `should return bad request for malformed schedule time`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            scheduleRulesJson = """
              "scheduleRules": [{
                  "startTime": "INVALID",
                  "endTime": "12:30",
                  "monday": true,
                  "tuesday": true,
                  "wednesday": true,
                  "thursday": true,
                  "friday": true,
                  "saturday": false,
                  "sunday": false
              }],
            """.trimIndent(),
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("INVALID")
          }
      }

      @Test
      fun `should return bad request if schedule rules fail validation`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            scheduleRulesJson = """
              "scheduleRules": [{
                  "startTime": "13:00",
                  "endTime": "12:30",
                  "monday": true,
                  "tuesday": true,
                  "wednesday": true,
                  "thursday": true,
                  "friday": true,
                  "saturday": false,
                  "sunday": false
              }],
            """.trimIndent(),
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Schedule rule has times out of order - 13:00 to 12:30")
          }
      }

      @Test
      fun `should return OK if schedule rules updated`() {
        val existingActivityId = getSavedActivityId()
        val existingRuleId = getSavedRuleId()

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(
            scheduleRulesJson = """
              "scheduleRules": [{
                  "startTime": "09:00",
                  "endTime": "12:00",
                  "monday": true,
                  "tuesday": true,
                  "wednesday": true,
                  "thursday": true,
                  "friday": false,
                  "saturday": false,
                  "sunday": false
              }],
            """.trimIndent(),
          ),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.courseScheduleRules.size).isEqualTo(1)
        with(updated.courseScheduleRules[0]) {
          assertThat(startTime.toLocalTime()).isEqualTo("09:00")
          assertThat(endTime?.toLocalTime()).isEqualTo("12:00")
          assertThat(this.friday).isEqualTo(false)
        }

        verify(telemetryClient).trackEvent(
          eq("activity-updated"),
          check<MutableMap<String, String>> {
            assertThat(it["nomisCourseActivityId"]).isEqualTo(existingActivityId.toString())
            assertThat(it["prisonId"]).isEqualTo("LEI")
            assertThat(it["removed-courseScheduleRuleIds"]).isEqualTo("[$existingRuleId]")
            assertThat(it["created-courseScheduleRuleIds"]).isEqualTo("[${updated.courseScheduleRules.first().id}]")
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class UpdateSchedules {
      @Test
      fun `invalid date should return bad request`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            schedulesJson = """
              "schedules":[
                {
                  "id": "${courseActivity.courseSchedules[0].courseScheduleId}",
                  "date": "2022-13-01",
                  "startTime": "09:00",
                  "endTime": "12:00"
                }
              ]
            """.trimIndent(),
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("2022-13-01")
          }
      }

      @Test
      fun `invalid start time should return bad request`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            schedulesJson = """
              "schedules": [
                {
                  "id": "${courseActivity.courseSchedules[0].courseScheduleId}",
                  "date": "$tomorrow",
                  "startTime": "09:70",
                  "endTime": "12:00"
                }
              ]
            """.trimIndent(),
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("09:70")
          }
      }

      @Test
      fun `invalid end time should return bad request`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            schedulesJson = """
              "schedules": [{
                  "id": "${courseActivity.courseSchedules[0].courseScheduleId}",
                  "date": "$tomorrow",
                  "startTime": "09:00",
                  "endTime": "25:00"
                }]
            """.trimIndent(),
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("25:00")
          }
      }

      @Test
      fun `updates should be saved to the database`() {
        val schedules = listOf(
          CourseScheduleBuilder(scheduleDate = today.toString()),
          CourseScheduleBuilder(scheduleDate = tomorrow.toString()),
        )
        courseActivity = repository.save(courseActivityBuilderFactory.builder(courseSchedules = schedules))
        callUpdateEndpoint(
          courseActivityId = courseActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(
            schedulesJson = """
              "schedules": [
                {
                  "id": ${courseActivity.courseSchedules[0].courseScheduleId},
                  "date": "$today",
                  "startTime": "08:00",
                  "endTime": "11:30"
                },
                {
                  "id": ${courseActivity.courseSchedules[1].courseScheduleId},
                  "date": "$tomorrow",
                  "startTime": "13:00",
                  "endTime": "15:00"
                }
              ]
            """.trimIndent(),
          ),
        )
          .expectStatus().isOk

        val saved = repository.lookupActivity(courseActivity.courseActivityId)
        assertThat(saved.courseSchedules.size).isEqualTo(2)
        with(saved.courseSchedules.first { it.scheduleDate == today }) {
          assertThat(startTime).isEqualTo("${today}T08:00:00")
          assertThat(endTime).isEqualTo("${today}T11:30:00")
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
        with(saved.courseSchedules.first { it.scheduleDate == tomorrow }) {
          assertThat(startTime).isEqualTo("${tomorrow}T13:00")
          assertThat(endTime).isEqualTo("${tomorrow}T15:00")
          assertThat(slotCategory).isEqualTo(SlotCategory.PM)
        }
      }

      @Test
      fun `do not delete any old schedules`() {
        val schedules = listOf(
          CourseScheduleBuilder(scheduleDate = yesterday.toString()),
          CourseScheduleBuilder(scheduleDate = today.toString()),
        )
        courseActivity = repository.save(courseActivityBuilderFactory.builder(courseSchedules = schedules))
        callUpdateEndpoint(
          courseActivityId = courseActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(
            schedulesJson = """
              "schedules": [
                {
                  "id": ${courseActivity.courseSchedules[1].courseScheduleId},
                  "date": "$today",
                  "startTime": "08:00",
                  "endTime": "11:00"
                }
              ]
            """.trimIndent(),
          ),
        )
          .expectStatus().isOk

        val saved = repository.lookupActivity(courseActivity.courseActivityId)
        assertThat(saved.courseSchedules.size).isEqualTo(2)
        // The course from yesterday was omitted from the request but is not deleted
        with(saved.courseSchedules.first { it.scheduleDate == yesterday }) {
          assertThat(startTime).isEqualTo("${yesterday}T08:00:00")
          assertThat(endTime).isEqualTo("${yesterday}T11:00:00")
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
      }

      @Test
      fun `cancellations should be saved to the database`() {
        val schedules = listOf(
          CourseScheduleBuilder(scheduleDate = today.toString()),
          CourseScheduleBuilder(scheduleDate = tomorrow.toString()),
        )
        courseActivity = repository.save(courseActivityBuilderFactory.builder(courseSchedules = schedules))
        callUpdateEndpoint(
          courseActivityId = courseActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(
            schedulesJson = """
              "schedules": [
                {
                  "id": ${courseActivity.courseSchedules[0].courseScheduleId},
                  "date": "$today",
                  "startTime": "08:00",
                  "endTime": "11:00"
                },
                {
                  "id": ${courseActivity.courseSchedules[1].courseScheduleId},
                  "date": "$tomorrow",
                  "startTime": "08:00",
                  "endTime": "11:00",
                  "cancelled": "true"
                }
              ]
            """.trimIndent(),
          ),
        )
          .expectStatus().isOk

        val saved = repository.lookupActivity(courseActivity.courseActivityId)
        assertThat(saved.courseSchedules.size).isEqualTo(2)
        with(saved.courseSchedules.first { it.scheduleDate == today }) {
          assertThat(startTime).isEqualTo("${today}T08:00:00")
          assertThat(endTime).isEqualTo("${today}T11:00:00")
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
        with(saved.courseSchedules.first { it.scheduleDate == tomorrow }) {
          assertThat(scheduleStatus).isEqualTo("CANC")
        }
      }

      @Test
      fun `service validation errors should return bad request`() {
        callUpdateEndpoint(
          courseActivityId = getSavedActivityId(),
          jsonBody = updateActivityRequestJson(
            schedulesJson = """
              "schedules": [
                {
                  "id": ${courseActivity.courseSchedules[0].courseScheduleId},
                  "date": "2022-11-01",
                  "startTime": "08:00",
                  "endTime": "11:00"
                },
                {
                  "date": "$tomorrow",
                  "startTime": "09:00",
                  "endTime": "01:00"
                }
              ]
            """.trimIndent(),
          ),
        )
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Schedule for date $tomorrow has times out of order - 09:00 to 01:00")
          }
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
        assertThat(updated.scheduleStartDate).isEqualTo(LocalDate.parse("2022-11-01"))
        assertThat(updated.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
        assertThat(updated.internalLocation?.locationId).isEqualTo(-27)
        assertThat(updated.capacity).isEqualTo(30)
        assertThat(updated.description).isEqualTo("updated description")
        assertThat(updated.iepLevel.code).isEqualTo("BAS")
        assertThat(updated.payPerSession).isEqualTo(PayPerSession.F)
        assertThat(updated.excludeBankHolidays).isTrue()
      }

      @Test
      fun `should allow nullable updates`() {
        val existingActivityId = getSavedActivityId()

        callUpdateEndpoint(
          courseActivityId = existingActivityId,
          jsonBody = updateActivityRequestJson(
            detailsJson = detailsJson()
              .replace(""""endDate" : "2022-11-30",""", "")
              .replace(""""internalLocationId" : -27,""", ""),
          ),
        )
          .expectStatus().isOk

        val updated = repository.lookupActivity(existingActivityId)
        assertThat(updated.scheduleEndDate).isNull()
        assertThat(updated.internalLocation).isNull()
      }

      @Test
      fun `should update program for activity and active allocations`() {
        val existingActivity = getSavedActivity()
        // create another program to move the activity to
        repository.save(ProgramServiceBuilder(programId = 30, programCode = "NEW_SERVICE"))
        // add an allocated offender who should be moved to the new program service
        val offenderBooking =
          repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID))).latestBooking()
        repository.save(offenderProgramProfileBuilderFactory.builder(), offenderBooking, existingActivity)
        // add a deallocated offender who should NOT be moved to the new program service
        val deallocatedOffenderBooking =
          repository.save(OffenderBuilder(nomsId = "A1234UU").withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID))).latestBooking()
        repository.save(offenderProgramProfileBuilderFactory.builder(endDate = LocalDate.now().minusDays(1).toString()), deallocatedOffenderBooking, existingActivity)

        callUpdateEndpoint(
          courseActivityId = existingActivity.courseActivityId,
          jsonBody = updateActivityRequestJson(
            detailsJson = detailsJson()
              .replace(""""programCode": "INTTEST",""", """"programCode": "NEW_SERVICE","""),
          ),
        )
          .expectStatus().isOk

        repository.runInTransaction {
          val updated = repository.lookupActivity(existingActivity.courseActivityId)
          assertThat(updated.program.programCode).isEqualTo("NEW_SERVICE")
          assertThat(updated.getProgramCode(offenderBooking.bookingId)).isEqualTo("NEW_SERVICE")
          assertThat(updated.getProgramCode(deallocatedOffenderBooking.bookingId)).isEqualTo("INTTEST")
        }
      }
    }

    @Nested
    inner class Response {
      @Test
      fun `should return course schedule details`() {
        courseActivity = repository.save(courseActivityBuilderFactory.builder(courseSchedules = listOf(CourseScheduleBuilder(scheduleDate = today.toString()))))
        val request = updateActivityRequestJson(
          schedulesJson =
          """
              "schedules": [
                {
                  "id": ${courseActivity.courseSchedules[0].courseScheduleId},
                  "date": "$today",
                  "startTime": "08:00",
                  "endTime": "11:00"
                },
                {
                  "date": "$tomorrow",
                  "startTime": "13:00",
                  "endTime": "15:00"
                }
              ]
          """.trimIndent(),
        )
        callUpdateEndpoint(courseActivityId = courseActivity.courseActivityId, jsonBody = request)
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseActivityId").value<Int> { assertThat(it).isEqualTo(courseActivity.courseActivityId) }
          .jsonPath("courseSchedules[0].courseScheduleId").value<Int> { assertThat(it).isEqualTo(courseActivity.courseSchedules[0].courseScheduleId) }
          .jsonPath("courseSchedules[0].date").isEqualTo("$today")
          .jsonPath("courseSchedules[0].startTime").isEqualTo("08:00:00")
          .jsonPath("courseSchedules[0].endTime").isEqualTo("11:00:00")
          .jsonPath("courseSchedules[1].courseScheduleId").value<Int> { assertThat(it).isGreaterThan(0) }
          .jsonPath("courseSchedules[1].date").isEqualTo("$tomorrow")
          .jsonPath("courseSchedules[1].startTime").isEqualTo("13:00:00")
          .jsonPath("courseSchedules[1].endTime").isEqualTo("15:00:00")
      }
    }

    private fun CourseActivity.getProgramCode(bookingId: Long): String =
      offenderProgramProfiles.first { it.offenderBooking.bookingId == bookingId }.program.programCode!!

    private fun callUpdateEndpoint(courseActivityId: Long, jsonBody: String) =
      webTestClient.put().uri("/activities/$courseActivityId")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonBody))
        .exchange()

    private fun getSavedActivityId() =
      repository.activityRepository.findAll().firstOrNull()?.courseActivityId
        ?: throw BadDataException("No activities in database")

    private fun getSavedActivity() =
      repository.activityRepository.findAll().firstOrNull()
        ?: throw BadDataException("No activities in database")

    private fun getSavedRuleId() =
      repository.runInTransaction {
        repository.activityRepository.findAll().firstOrNull()?.courseScheduleRules?.firstOrNull()?.id
          ?: throw BadDataException("Could not find course schedule rule in database")
      }
  }

  @Nested
  inner class DeleteActivity {

    @Autowired
    private lateinit var courseActivityAreaRepository: CourseActivityAreaRepository

    private fun createRequestJson() = """{
            "prisonId" : "$PRISON_ID",
            "code" : "CA",
            "programCode" : "$PROGRAM_CODE",
            "description" : "test description",
            "capacity" : 23,
            "startDate" : "2022-10-31",
            "minimumIncentiveLevelCode" : "$IEP_LEVEL",
            "internalLocationId" : "$ROOM_ID",
            "payRates" : [
              {
                "incentiveLevel" : "BAS",
                "payBand" : "4",
                "rate" : 0.4
              },
              {
                "incentiveLevel" : "BAS",
                "payBand" : "5",
                "rate" : 0.5
              }
            ],
            "payPerSession": "F",
            "schedules" : [ 
              {
                "date": "2022-10-31",
                "startTime" : "09:00",
                "endTime" : "11:00"
              },
              {
                "date": "2022-11-30",
                "startTime" : "14:00",
                "endTime" : "15:00"
              }
            ],
            "scheduleRules": [
              {
                "startTime": "09:00",
                "endTime": "11:00",
                "monday": false,
                "tuesday": true,
                "wednesday": false,
                "thursday": true,
                "friday": true,
                "saturday": false,
                "sunday": false
              },
              {
                "startTime": "14:00",
                "endTime": "15:00",
                "monday": true,
                "tuesday": true,
                "wednesday": true,
                "thursday": false,
                "friday": false,
                "saturday": false,
                "sunday": false
              }
            ]
          }
    """.trimIndent()

    fun allocateOffenderJson(bookingId: Long) = """
      {
        "bookingId": $bookingId,
        "startDate": "2022-12-01",
        "payBandCode": "4",
        "programStatusCode": "ALLOC"
      }
    """.trimIndent()

    fun updateRequestJson() = """
      "internalLocationId" : ${ROOM_ID + 1},
      "payRates" : [
        {
          "incentiveLevel" : "BAS",
          "payBand" : "4",
          "rate" : 0.4
        },
        {
          "incentiveLevel" : "BAS",
          "payBand" : "5",
          "rate" : 0.5
        }
      ],
      "scheduleRules": [
        {
          "startTime": "09:00",
          "endTime": "11:00",
          "monday": false,
          "tuesday": true,
          "wednesday": false,
          "thursday": true,
          "friday": true,
          "saturday": false,
          "sunday": false
        },
        {
          "startTime": "14:00",
          "endTime": "15:00",
          "monday": true,
          "tuesday": true,
          "wednesday": true,
          "thursday": false,
          "friday": false,
          "saturday": false,
          "sunday": false
        }
      ]
    """.trimIndent()

    @Test
    fun `should delete activity and activity area`() {
      // create activity
      val activityId = webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createRequestJson()))
        .exchange()
        .expectStatus().isCreated
        .expectBody(ActivityResponse::class.java)
        .returnResult().responseBody
        ?.courseActivityId!!

      // Emulate the Nomis trigger COURSE_ACTIVITIES_T2.trg
      repository.runInTransaction {
        val activity = repository.lookupActivity(activityId)
        activity.area = CourseActivityArea(activityId, activity, "AREA")
        repository.activityRepository.save(activity)
      }

      // allocate offender
      val offenderAtMoorlands =
        repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID)))
      webTestClient.put().uri("/activities/$activityId/allocation")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(allocateOffenderJson(offenderAtMoorlands.latestBooking().bookingId)))
        .exchange()
        .expectStatus().isOk

      val savedActivity = repository.lookupActivity(activityId)
      val savedAllocation = repository.lookupOffenderProgramProfile(savedActivity, offenderAtMoorlands.latestBooking())
      assertThat(savedAllocation).isNotEmpty

      // delete activity and deallocate
      webTestClient.delete().uri("/activities/$activityId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNoContent

      // check everything is deleted
      assertThat(repository.activityRepository.findByIdOrNull(activityId)).isNull()
      assertThat(repository.offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(savedActivity, offenderAtMoorlands.latestBooking())).isEmpty()
      assertThat(courseActivityAreaRepository.findByIdOrNull(activityId)).isNull()
    }

    @Test
    fun `should delete activity and attendance`() {
      // create activity
      val activityId = webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createRequestJson()))
        .exchange()
        .expectStatus().isCreated
        .expectBody(ActivityResponse::class.java)
        .returnResult().responseBody
        ?.courseActivityId!!

      // allocate offender
      val offenderAtMoorlands =
        repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID)))
      webTestClient.put().uri("/activities/$activityId/allocation")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(allocateOffenderJson(offenderAtMoorlands.latestBooking().bookingId)))
        .exchange()
        .expectStatus().isOk

      val savedActivity = repository.lookupActivity(activityId)
      val savedSchedule = repository.lookupActivity(activityId).courseSchedules.firstOrNull()
      assertThat(savedSchedule).isNotNull
      val savedAllocation = repository.offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(savedActivity, offenderAtMoorlands.latestBooking()).last()
      assertThat(savedAllocation).isNotNull

      // create an attendance record
      repository.runInTransaction {
        repository.save(offenderCourseAttendanceBuilderFactory.builder(), savedSchedule!!, savedAllocation)
      }

      val savedAttendances = repository.offenderCourseAttendanceRepository.findByCourseScheduleAndOffenderBooking(savedSchedule!!, offenderAtMoorlands.latestBooking())
      assertThat(savedAttendances).isNotNull

      // delete activity, allocation and attendance
      webTestClient.delete().uri("/activities/$activityId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNoContent

      // check everything is deleted
      assertThat(repository.activityRepository.findByIdOrNull(activityId)).isNull()
      assertThat(repository.offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(savedActivity, offenderAtMoorlands.latestBooking())).isEmpty()
      assertThat(repository.offenderCourseAttendanceRepository.findByCourseScheduleAndOffenderBooking(savedSchedule, offenderAtMoorlands.latestBooking())).isNull()
    }

    @Test
    fun `should retain activity area after an update`() {
      // create activity
      val activityId = webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(createRequestJson()))
        .exchange()
        .expectStatus().isCreated
        .expectBody(ActivityResponse::class.java)
        .returnResult().responseBody
        ?.courseActivityId!!

      // Emulate the Nomis trigger COURSE_ACTIVITIES_T2.trg
      repository.runInTransaction {
        val activity = repository.lookupActivity(activityId)
        activity.area = CourseActivityArea(activityId, activity, "AREA")
        repository.activityRepository.save(activity)
      }

      // update the activity
      webTestClient.put().uri("/activities/$activityId")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(updateRequestJson()))
        .exchange()

      // check the activity still has an activity area
      val savedActivity = repository.lookupActivity(activityId)
      assertThat(savedActivity.area!!.areaCode).isEqualTo("AREA")
    }
  }
}
