package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAttendanceResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderCourseAttendanceBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderProgramProfileBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

class AttendanceResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var courseActivityBuilderFactory: CourseActivityBuilderFactory

  @Autowired
  private lateinit var allocationBuilderFactory: OffenderProgramProfileBuilderFactory

  @Autowired
  private lateinit var attendanceBuilderFactory: OffenderCourseAttendanceBuilderFactory

  @Nested
  inner class CreateAttendance {
    @Nested
    inner class Api {

      private val validJsonRequest = """
        {
          "scheduleDate": "${LocalDateTime.now().plusDays(1)}",
          "startTime": "10:00",
          "endTime": "11:00",
          "eventStatusCode": "COMP",
          "eventOutcomeCode": "ATT",
          "comments": "Engaged",
          "unexcusedAbsence": false,
          "authorisedAbsence": "false",
          "paid": "true",
          "bonusPay": 1.5
        }
      """.trimIndent()

      @Test
      fun `should return unauthorised if no token`() {
        webTestClient.post().uri("/activities/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden if no role`() {
        webTestClient.post().uri("/activities/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden with wrong role`() {
        webTestClient.post().uri("/activities/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found`() {
        webTestClient.post().uri("/activities/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `should return bad request`() {
        webTestClient.post().uri("/activities/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest.replace(""""unexcusedAbsence": false,""", """"unexcusedAbsence": INVALID,"""),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("INVALID")
          }
      }
    }

    @Nested
    inner class Validation {

      private lateinit var courseActivity: CourseActivity
      private lateinit var courseSchedule: CourseSchedule
      private lateinit var offender: Offender
      private lateinit var offenderBooking: OffenderBooking

      private val validJsonRequest = """
        {
          "scheduleDate": "2022-11-01",
          "startTime": "08:00",
          "endTime": "11:00",
          "eventStatusCode": "SCH",
          "eventOutcomeCode": null,
          "comments": null,
          "unexcusedAbsence": null,
          "authorisedAbsence": null,
          "paid": null,
          "bonusPay": null
        }
      """.trimIndent()

      @BeforeEach
      fun setUp() {
        repository.save(ProgramServiceBuilder())
        courseActivity = repository.save(courseActivityBuilderFactory.builder())
        courseSchedule = courseActivity.courseSchedules.first()
        offender = repository.save(OffenderBuilder(nomsId = "A1234AR").withBooking(OffenderBookingBuilder(agencyLocationId = "LEI")))
        offenderBooking = offender.latestBooking()
      }

      @AfterEach
      fun cleanUp() {
        repository.deleteAttendances()
        repository.deleteOffenders()
        repository.deleteActivities()
        repository.deleteProgramServices()
      }

      @Test
      fun `should return not found if activity not found`() {
        webTestClient.createAttendance(111, offenderBooking.bookingId)
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course activity with id=111 not found")
          }
      }

      @Test
      fun `should return not found if course schedule not found`() {
        val jsonRequest = validJsonRequest.replace(""""endTime": "11:00",""", """"endTime": "12:00",""")

        webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course schedule for activity=${courseActivity.courseActivityId}, date=2022-11-01, start time=08:00 and end time=12:00 not found")
          }
      }

      @Test
      fun `should return not found if offender booking not found`() {
        webTestClient.createAttendance(courseActivity.courseActivityId, 222)
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender booking with id=222 not found")
          }
      }

      @Test
      fun `should return not found if allocation not found`() {
        webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender program profile for offender booking with id=${offenderBooking.bookingId} and course activity id=${courseActivity.courseActivityId} not found")
          }
      }

      @Test
      fun `should return conflict if scheduled attendance already exists`() {
        val allocation = repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val attendance = repository.save(attendanceBuilderFactory.builder(eventStatusCode = "SCH"), courseSchedule, allocation)

        webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isEqualTo(409)
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender course attendance already exists with eventId=${attendance.eventId} and eventStatus=SCH")
          }
      }

      @Test
      fun `should return conflict if complete attendance already exists`() {
        val allocation = repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val attendance = repository.save(attendanceBuilderFactory.builder(eventStatusCode = "COMP"), courseSchedule, allocation)

        webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isEqualTo(409)
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender course attendance already exists with eventId=${attendance.eventId} and eventStatus=COMP")
          }
      }

      @Test
      fun `should return bad request if event status code invalid`() {
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val jsonRequest = validJsonRequest.replace(""""eventStatusCode": "SCH",""", """"eventStatusCode": "INVALID",""")

        webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Event status code INVALID does not exist")
          }
      }

      @Test
      fun `should return bad request if attendance outcome code invalid`() {
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val jsonRequest = validJsonRequest.replace(""""eventOutcomeCode": null,""", """"eventOutcomeCode": "INVALID",""")

        webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Attendance outcome code INVALID does not exist")
          }
      }

      @Test
      fun `should return created for valid request`() {
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)

        val response = webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isCreated
          .expectBody(CreateAttendanceResponse::class.java)
          .returnResult().responseBody!!

        val saved = repository.lookupAttendance(response.eventId)
        with(saved) {
          assertThat(offenderBooking.bookingId).isEqualTo(this@Validation.offenderBooking.bookingId)
          assertThat(eventDate).isEqualTo("2022-11-01")
          assertThat(startTime).isEqualTo("2022-11-01T08:00")
          assertThat(endTime).isEqualTo("2022-11-01T11:00")
          assertThat(inTime).isEqualTo("2022-11-01T08:00")
          assertThat(outTime).isEqualTo("2022-11-01T11:00")
          assertThat(eventStatus.code).isEqualTo("SCH")
          assertThat(courseSchedule?.courseScheduleId).isEqualTo(this@Validation.courseSchedule.courseScheduleId)
          assertThat(toInternalLocation?.locationId).isEqualTo(-8)
          assertThat(courseActivity?.courseActivityId).isEqualTo(this@Validation.courseActivity.courseActivityId)
          assertThat(prison?.id).isEqualTo("LEI")
          assertThat(program?.programId).isEqualTo(20)
        }
      }

      @Test
      fun `should populate data from request`() {
        val jsonRequest = """
          {
            "scheduleDate": "2022-11-01",
            "startTime": "08:00",
            "endTime": "11:00",
            "eventStatusCode": "COMP",
            "eventOutcomeCode": "ATT",
            "comments": "Attended",
            "unexcusedAbsence": "false",
            "authorisedAbsence": "false",
            "paid": "true",
            "bonusPay": "1.50"
          }
        """.trimIndent()
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)

        val response = webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isCreated
          .expectBody(CreateAttendanceResponse::class.java)
          .returnResult().responseBody!!

        val saved = repository.lookupAttendance(response.eventId)
        with(saved) {
          assertThat(eventStatus.code).isEqualTo("COMP")
          assertThat(attendanceOutcome?.code).isEqualTo("ATT")
          assertThat(commentText).isEqualTo("Attended")
          assertThat(unexcusedAbsence).isEqualTo(false)
          assertThat(authorisedAbsence).isEqualTo(false)
          assertThat(paid).isEqualTo(true)
          assertThat(bonusPay).isEqualTo(BigDecimal(1.5).setScale(3, RoundingMode.HALF_UP))
        }
      }

      @Test
      fun `should create attendance if cancelled attendance already exists`() {
        val allocation = repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        repository.save(attendanceBuilderFactory.builder(eventStatusCode = "CANC"), courseSchedule, allocation)

        val response = webTestClient.createAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isCreated
          .expectBody(CreateAttendanceResponse::class.java)
          .returnResult().responseBody!!

        val saved = repository.lookupAttendance(response.eventId)
        assertThat(saved.eventStatus.code).isEqualTo("SCH")
      }

      private fun WebTestClient.createAttendance(courseActivityId: Long, bookingId: Long, jsonRequest: String = validJsonRequest) =
        post().uri("/activities/$courseActivityId/booking/$bookingId/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(jsonRequest))
          .exchange()
    }
  }
}
