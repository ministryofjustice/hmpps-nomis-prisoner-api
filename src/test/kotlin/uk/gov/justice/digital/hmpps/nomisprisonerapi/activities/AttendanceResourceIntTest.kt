package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceResponse
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import java.math.BigDecimal
import java.math.RoundingMode

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
  inner class UpsertAttendance {

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
      offender =
        repository.save(OffenderBuilder(nomsId = "A1234AR").withBooking(OffenderBookingBuilder(agencyLocationId = "LEI")))
      offenderBooking = offender.latestBooking()
    }

    @AfterEach
    fun cleanUp() {
      repository.deleteAttendances()
      repository.deleteOffenders()
      repository.deleteActivities()
      repository.deleteProgramServices()
    }

    @Nested
    inner class Api {
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
      fun `should return bad request`() {
        webTestClient.post().uri("/activities/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest.replace(""""unexcusedAbsence": null,""", """"unexcusedAbsence": INVALID,"""),
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

      @Test
      fun `should return bad request if activity not found`() {
        webTestClient.upsertAttendance(111, offenderBooking.bookingId)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course activity with id=111 not found")
          }
      }

      @Test
      fun `should return bad request if course schedule not found`() {
        val jsonRequest = validJsonRequest.replace(""""endTime": "11:00",""", """"endTime": "12:00",""")

        webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course schedule for activity=${courseActivity.courseActivityId}, date=2022-11-01, start time=08:00 and end time=12:00 not found")
          }
      }

      @Test
      fun `should return bad request if offender booking not found`() {
        webTestClient.upsertAttendance(courseActivity.courseActivityId, 222)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender booking with id=222 not found")
          }
      }

      @Test
      fun `should return bad request if allocation not found`() {
        webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender program profile for offender booking with id=${offenderBooking.bookingId} and course activity id=${courseActivity.courseActivityId} not found")
          }
      }

      @Test
      fun `should return bad request if event status code invalid`() {
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val jsonRequest = validJsonRequest.replace(""""eventStatusCode": "SCH",""", """"eventStatusCode": "INVALID",""")

        webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Event status code INVALID does not exist")
          }
      }

      @Test
      fun `should return bad request if attendance outcome code invalid`() {
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val jsonRequest =
          validJsonRequest.replace(""""eventOutcomeCode": null,""", """"eventOutcomeCode": "INVALID",""")

        webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Attendance outcome code INVALID does not exist")
          }
      }

      @Test
      fun `should return bad request if attendance already paid`() {
        val allocation = repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val attendance = saveAttendance("COMP", courseSchedule, allocation, paidTransactionId = 123456)

        webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Attendance ${attendance.eventId} cannot be changed after it has already been paid")
          }
      }
    }

    @Nested
    inner class SaveAttendance {

      @Test
      fun `should return OK if created new attendance`() {
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)

        val response = webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isOk
          .expectBody(UpsertAttendanceResponse::class.java)
          .returnResult().responseBody!!

        assertThat(response.courseScheduleId).isEqualTo(courseSchedule.courseScheduleId)
        assertThat(response.created).isTrue()

        val saved = repository.lookupAttendance(response.eventId)
        with(saved) {
          assertThat(offenderBooking.bookingId).isEqualTo(this@UpsertAttendance.offenderBooking.bookingId)
          assertThat(eventDate).isEqualTo("2022-11-01")
          assertThat(startTime).isEqualTo("2022-11-01T08:00")
          assertThat(endTime).isEqualTo("2022-11-01T11:00")
          assertThat(inTime).isEqualTo("2022-11-01T08:00")
          assertThat(outTime).isEqualTo("2022-11-01T11:00")
          assertThat(eventStatus.code).isEqualTo("SCH")
          assertThat(courseSchedule.courseScheduleId).isEqualTo(this@UpsertAttendance.courseSchedule.courseScheduleId)
          assertThat(toInternalLocation?.locationId).isEqualTo(-8)
          assertThat(courseActivity.courseActivityId).isEqualTo(this@UpsertAttendance.courseActivity.courseActivityId)
          assertThat(prison?.id).isEqualTo("LEI")
          assertThat(program?.programId).isEqualTo(20)
          assertThat(referenceId).isEqualTo(this@UpsertAttendance.courseSchedule.courseScheduleId)
        }
      }

      @Test
      fun `should return OK if updated existing attendance`() {
        val allocation = repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val attendance = saveAttendance("SCH", courseSchedule, allocation)

        webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isOk
          .expectBody()
          .jsonPath("eventId").isEqualTo(attendance.eventId)
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)
          .jsonPath("created").isEqualTo("false")

        val saved = repository.lookupAttendance(attendance.eventId)
        assertThat(saved.eventStatus.code).isEqualTo("SCH")
      }

      @Test
      fun `should publish telemetry when creating`() {
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)

        val response = webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isOk
          .expectBody(UpsertAttendanceResponse::class.java)
          .returnResult().responseBody!!

        verify(telemetryClient).trackEvent(
          eq("activity-attendance-created"),
          check<MutableMap<String, String>> {
            assertThat(it["nomisCourseActivityId"]).isEqualTo(courseActivity.courseActivityId.toString())
            assertThat(it["nomisCourseScheduleId"]).isEqualTo(courseSchedule.courseScheduleId.toString())
            assertThat(it["bookingId"]).isEqualTo(offenderBooking.bookingId.toString())
            assertThat(it["offenderNo"]).isEqualTo(offenderBooking.offender.nomsId)
            assertThat(it["nomisAttendanceEventId"]).isEqualTo(response.eventId.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `should publish telemetry when updating`() {
        val allocation = repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        val attendance = saveAttendance("SCH", courseSchedule, allocation)

        webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId)
          .expectStatus().isOk
          .expectBody()

        verify(telemetryClient).trackEvent(
          eq("activity-attendance-updated"),
          check<MutableMap<String, String>> {
            assertThat(it["nomisCourseActivityId"]).isEqualTo(courseActivity.courseActivityId.toString())
            assertThat(it["nomisCourseScheduleId"]).isEqualTo(courseSchedule.courseScheduleId.toString())
            assertThat(it["bookingId"]).isEqualTo(offenderBooking.bookingId.toString())
            assertThat(it["offenderNo"]).isEqualTo(offenderBooking.offender.nomsId)
            assertThat(it["nomisAttendanceEventId"]).isEqualTo(attendance.eventId.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `should populate data from request when creating`() {
        val jsonRequest = """
          {
            "scheduleDate": "2022-11-01",
            "startTime": "08:00",
            "endTime": "11:00",
            "eventStatusCode": "COMP",
            "eventOutcomeCode": "CANC",
            "comments": "Cancelled",
            "unexcusedAbsence": "false",
            "authorisedAbsence": "true",
            "paid": "true",
            "bonusPay": "1.50"
          }
        """.trimIndent()
        repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)

        val response =
          webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
            .expectStatus().isOk
            .expectBody(UpsertAttendanceResponse::class.java)
            .returnResult().responseBody!!

        val saved = repository.lookupAttendance(response.eventId)
        with(saved) {
          assertThat(eventStatus.code).isEqualTo("COMP")
          assertThat(attendanceOutcome?.code).isEqualTo("CANC")
          assertThat(commentText).isEqualTo("Cancelled")
          assertThat(unexcusedAbsence).isEqualTo(false)
          assertThat(authorisedAbsence).isEqualTo(true)
          assertThat(paid).isEqualTo(true)
          assertThat(bonusPay).isEqualTo(BigDecimal(1.5).setScale(3, RoundingMode.HALF_UP))
          assertThat(performanceCode).isNull()
        }
      }

      @Test
      fun `should populate data from request when updating`() {
        val allocation = repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
        saveAttendance("SCH", courseSchedule, allocation)
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

        val response =
          webTestClient.upsertAttendance(courseActivity.courseActivityId, offenderBooking.bookingId, jsonRequest)
            .expectStatus().isOk
            .expectBody(UpsertAttendanceResponse::class.java)
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
          assertThat(performanceCode).isEqualTo("STANDARD")
        }
      }
    }

    private fun WebTestClient.upsertAttendance(
      courseActivityId: Long,
      bookingId: Long,
      jsonRequest: String = validJsonRequest,
    ) =
      post().uri("/activities/$courseActivityId/booking/$bookingId/attendance")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
  }

  @Nested
  inner class GetAttendanceStatus {

    private lateinit var courseActivity: CourseActivity
    private lateinit var courseSchedule: CourseSchedule
    private lateinit var offender: Offender
    private lateinit var offenderBooking: OffenderBooking

    val jsonRequest = """
      {
        "scheduleDate": "2022-11-01",
        "startTime": "08:00",
        "endTime": "11:00"
      }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
      repository.save(ProgramServiceBuilder())
      courseActivity = repository.save(courseActivityBuilderFactory.builder())
      courseSchedule = courseActivity.courseSchedules.first()
      offender =
        repository.save(OffenderBuilder(nomsId = "A1234AR").withBooking(OffenderBookingBuilder(agencyLocationId = "LEI")))
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
    fun `should return OK if attendance status found`() {
      val allocation = repository.save(allocationBuilderFactory.builder(), offenderBooking, courseActivity)
      saveAttendance("SCH", courseSchedule, allocation)

      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}/booking/${offenderBooking.bookingId}/attendance-status")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("$.eventStatus").isEqualTo("SCH")
    }

    @Test
    fun `should return bad request if activity not found`() {
      webTestClient.post().uri("/activities/1111/booking/${offenderBooking.bookingId}/attendance-status")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Course activity with id=1111 not found")
        }
    }

    @Test
    fun `should return bad request if offender booking not found`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}/booking/2222/attendance-status")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Offender booking with id=2222 not found")
        }
    }

    @Test
    fun `should return not found if attendance not found`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}/booking/${offenderBooking.bookingId}/attendance-status")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Attendance for activity=${courseActivity.courseActivityId}, offender booking=${offenderBooking.bookingId}, date=2022-11-01, start time=08:00 and end time=11:00 not found")
        }
    }

    @Test
    fun `should return unauthorised if no token`() {
      webTestClient.post().uri("/activities/1/booking/2/attendance")
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden if no role`() {
      webTestClient.post().uri("/activities/1/booking/2/attendance-status")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden with wrong role`() {
      webTestClient.post().uri("/activities/1/booking/2/attendance-status")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  private fun saveAttendance(eventStatus: String, courseSchedule: CourseSchedule, allocation: OffenderProgramProfile, paidTransactionId: Long? = null) =
    with(courseSchedule) {
      repository.save(
        attendanceBuilderFactory.builder(
          eventStatusCode = eventStatus,
          eventDate = this.scheduleDate,
          startTime = this.startTime,
          endTime = this.endTime,
          paidTransactionId = paidTransactionId,
        ),
        courseSchedule,
        allocation,
      )
    }
}
