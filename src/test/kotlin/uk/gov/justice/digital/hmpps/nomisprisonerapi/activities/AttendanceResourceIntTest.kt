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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderCourseAttendanceBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.testData
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
  private lateinit var attendanceBuilderFactory: OffenderCourseAttendanceBuilderFactory

  @Nested
  inner class UpsertAttendance {

    private lateinit var courseActivity: CourseActivity
    private lateinit var courseSchedule: CourseSchedule
    private lateinit var offender: Offender
    private lateinit var offenderBooking: OffenderBooking
    private lateinit var allocation: OffenderProgramProfile

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

    private fun String.withScheduleDate(scheduleDate: String?) =
      replace(""""scheduleDate": "2022-11-01",""", scheduleDate?.let { """"scheduleDate": "$scheduleDate",""" } ?: "")

    private fun String.withStartTime(startTime: String?) =
      replace(""""startTime": "08:00",""", startTime?.let { """"startTime": "$startTime",""" } ?: "")

    private fun String.withEndTime(endTime: String?) =
      replace(""""endTime": "11:00",""", endTime?.let { """"endTime": "$endTime",""" } ?: "")

    private fun String.withEventStatusCode(eventStatusCode: String?) =
      replace(""""eventStatusCode": "SCH",""", eventStatusCode?.let { """"eventStatusCode": "$eventStatusCode",""" } ?: "")

    private fun String.withEventOutcomeCode(eventOutcomeCode: String?) =
      replace(""""eventOutcomeCode": null,""", eventOutcomeCode?.let { """"eventOutcomeCode": "$eventOutcomeCode",""" } ?: "")

    private fun String.withUnexcusedAbsence(unexcusedAbsence: String?) =
      replace(""""unexcusedAbsence": null,""", unexcusedAbsence?.let { """"unexcusedAbsence": $unexcusedAbsence,""" } ?: "")

    private fun String.withPaidFlag(paidFlag: String?) =
      replace(""""paid": null,""", paidFlag?.let { """"paid": $paidFlag,""" } ?: "")

    private fun String.withBonusPay(bonusPay: String?) =
      replace(""""bonusPay": null""", bonusPay?.let { """"bonusPay": $bonusPay""" } ?: "")

    @BeforeEach
    fun setUp() {
      testData(repository) {
        programService {
          courseActivity = courseActivity {
            payRate()
            courseSchedule()
            courseScheduleRule()
          }
        }
      }
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
        webTestClient.put().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden if no role`() {
        webTestClient.put().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden with wrong role`() {
        webTestClient.put().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return bad request`() {
        webTestClient.put().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest.withUnexcusedAbsence("INVALID"),
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
      fun `should return bad request if course schedule not found`() {
        webTestClient.upsertAttendance(9999, offenderBooking.bookingId)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course schedule for courseScheduleId=9999 not found")
          }
      }

      @Test
      fun `should return bad request if offender booking not found`() {
        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, 222)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender booking with id=222 not found")
          }
      }

      @Test
      fun `should return bad request if creating attendance for offender in wrong prison`() {
        testData(repository) {
          offender = offender(nomsId = "A1234TU") {
            booking(agencyLocationId = "MDI")
          }
          offenderBooking = offender.latestBooking()
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Prisoner is at prison=${offenderBooking.location?.id}, not the Course activity prison=${courseActivity.prison.id}")
          }
      }

      @Test
      fun `should return bad request if allocation not found`() {
        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender program profile for offender booking with id=${offenderBooking.bookingId} and course activity id=${courseActivity.courseActivityId} not found")
          }
      }

      @Test
      fun `should return bad request if attendance already paid`() {
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val attendance = saveAttendance("COMP", courseSchedule, allocation, paidTransactionId = 123456)

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Attendance ${attendance.eventId} cannot be changed after it has already been paid")
          }
      }

      @Test
      fun `should return bad request if creating an attendance and allocation has ended`() {
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity, programStatusCode = "END")
        }

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Cannot create an attendance for allocation ${allocation.offenderProgramReferenceId} because it has ended")
          }
      }

      @Test
      fun `should return bad request if schedule date not passed`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withScheduleDate(null)

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("scheduleDate")
          }
      }

      @Test
      fun `should return bad request if invalid schedule date`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withScheduleDate("2022-11-50")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("2022-11-50")
          }
      }

      @Test
      fun `should return bad request if start time not passed`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withStartTime(null)

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("startTime")
          }
      }

      @Test
      fun `should return bad request if invalid start time`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withStartTime("08:70")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("08:70")
          }
      }

      @Test
      fun `should return bad request if end time not passed`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withEndTime(null)

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("endTime")
          }
      }

      @Test
      fun `should return bad request if invalid end time`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withEndTime("25:00")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("25:00")
          }
      }

      @Test
      fun `should return bad request if event status code invalid`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withEventStatusCode("INVALID")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Event status code INVALID does not exist")
          }
      }

      @Test
      fun `should return bad request if attendance outcome code invalid`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withEventOutcomeCode("INVALID")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Attendance outcome code INVALID does not exist")
          }
      }

      @Test
      fun `should return bad request if paid flag invalid`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withPaidFlag("INVALID")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("INVALID")
          }
      }

      @Test
      fun `should return bad request if bonus pay invalid`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val jsonRequest = validJsonRequest.withBonusPay("INVALID")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
          .expectStatus().isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("INVALID")
          }
      }

      @Test
      fun `should return bad request if trying to update a paid attendance`() {
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val attendance = saveAttendance("COMP", courseSchedule, allocation, paidTransactionId = 123456)

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
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
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity) { payBand() }
        }

        val response = webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
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
      fun `should return OK if the prisoner has multiple allocations to the course`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity, programStatusCode = "END", endDate = "2022-10-31") { payBand() }
          courseAllocation(offenderBooking, courseActivity, startDate = "2022-11-01") { payBand() }
        }

        val response = webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
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
        }
      }

      @Test
      fun `should return OK if updated existing attendance`() {
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val attendance = saveAttendance("SCH", courseSchedule, allocation)

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
          .expectStatus().isOk
          .expectBody()
          .jsonPath("eventId").isEqualTo(attendance.eventId)
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)
          .jsonPath("created").isEqualTo("false")

        val saved = repository.lookupAttendance(attendance.eventId)
        assertThat(saved.eventStatus.code).isEqualTo("SCH")
      }

      @Test
      fun `should return OK if changing dates and times`() {
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val attendance = saveAttendance("SCH", courseSchedule, allocation)
        val request = validJsonRequest.withScheduleDate("2022-11-02")
          .withStartTime("13:00")
          .withEndTime("14:00")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, request)
          .expectStatus().isOk
          .expectBody()
          .jsonPath("eventId").isEqualTo(attendance.eventId)
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)
          .jsonPath("created").isEqualTo("false")

        val saved = repository.lookupAttendance(attendance.eventId)
        assertThat(saved.eventStatus.code).isEqualTo("SCH")
        assertThat(saved.eventDate).isEqualTo("2022-11-02")
        assertThat(saved.startTime).isEqualTo("2022-11-02T13:00")
        assertThat(saved.endTime).isEqualTo("2022-11-02T14:00")
      }

      @Test
      fun `should return OK if updating attendance for offender in wrong prison`() {
        testData(repository) {
          offender = offender(nomsId = "A1234TU") {
            booking(agencyLocationId = "MDI")
          }
          offenderBooking = offender.latestBooking()
          allocation = courseAllocation(offenderBooking, courseActivity) { payBand() }
        }
        val attendance = saveAttendance("SCH", courseSchedule, allocation)
        val request = validJsonRequest.withEventStatusCode("CANC")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, request)
          .expectStatus().isOk

        val saved = repository.lookupAttendance(attendance.eventId)
        assertThat(saved.eventStatus.code).isEqualTo("CANC")
      }

      @Test
      fun `should return OK if updating attendance and offender has been deallocated`() {
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity, programStatusCode = "END")
        }
        val attendance = saveAttendance("SCH", courseSchedule, allocation)
        val request = validJsonRequest.withEventStatusCode("EXP")

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, request)
          .expectStatus().isOk

        val saved = repository.lookupAttendance(attendance.eventId)
        assertThat(saved.eventStatus.code).isEqualTo("EXP")
      }

      @Test
      fun `should publish telemetry when creating`() {
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity)
        }

        val response = webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
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
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity)
        }
        val attendance = saveAttendance("SCH", courseSchedule, allocation)

        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
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
        testData(repository) {
          courseAllocation(offenderBooking, courseActivity)
        }
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

        val response =
          webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
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
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity)
        }
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
          webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId, jsonRequest)
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

    @Nested
    inner class DuplicateAttendance {
      @Test
      fun `duplicate attendance can be worked around by deleting one of them`() {
        testData(repository) {
          allocation = courseAllocation(offenderBooking, courseActivity)
        }
        saveAttendance("SCH", courseSchedule, allocation)
        val duplicate = saveAttendance("SCH", courseSchedule, allocation)

        // unable to update the attendance because of a duplicate
        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
          .expectStatus().is5xxServerError
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("query did not return a unique result")
          }

        // delete the duplicate
        webTestClient.delete().uri("/attendances/${duplicate.eventId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .exchange()
          .expectStatus().isNoContent

        // now able to update the attendance
        webTestClient.upsertAttendance(courseSchedule.courseScheduleId, offenderBooking.bookingId)
          .expectStatus().isOk
      }
    }

    private fun WebTestClient.upsertAttendance(
      courseScheduleId: Long,
      bookingId: Long,
      jsonRequest: String = validJsonRequest,
    ) =
      put().uri("/schedules/$courseScheduleId/booking/$bookingId/attendance")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(jsonRequest))
        .exchange()
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
