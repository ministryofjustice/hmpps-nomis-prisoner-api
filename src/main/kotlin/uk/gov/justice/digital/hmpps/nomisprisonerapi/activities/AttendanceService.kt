package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.AttendanceReconciliationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadRequestError
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AttendanceOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourseAttendanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Service
@Transactional
class AttendanceService(
  private val attendanceRepository: OffenderCourseAttendanceRepository,
  private val scheduleRepository: CourseScheduleRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val attendanceOutcomeRepository: ReferenceCodeRepository<AttendanceOutcome>,
  private val telemetryClient: TelemetryClient,
) {

  fun upsertAttendance(
    courseScheduleId: Long,
    bookingId: Long,
    upsertAttendanceRequest: UpsertAttendanceRequest,
  ): UpsertAttendanceResponse {
    val attendance = toOffenderCourseAttendance(courseScheduleId, bookingId, upsertAttendanceRequest)
    val created = attendance.eventId == 0L
    return attendanceRepository.save(attendance)
      .let { UpsertAttendanceResponse(it.eventId, it.courseSchedule.courseScheduleId, created, it.prison?.id ?: "") }
      .also {
        telemetryClient.trackEvent(
          "activity-attendance-${if (it.created) "created" else "updated"}",
          mapOf(
            "nomisCourseActivityId" to attendance.courseActivity.courseActivityId.toString(),
            "nomisCourseScheduleId" to attendance.courseSchedule.courseScheduleId.toString(),
            "bookingId" to attendance.offenderBooking.bookingId.toString(),
            "offenderNo" to attendance.offenderBooking.offender.nomsId,
            "nomisAttendanceEventId" to attendance.eventId.toString(),
            "prisonId" to attendance.courseActivity.prison.id,
          ),
          null,
        )
      }
  }

  private fun toOffenderCourseAttendance(
    courseScheduleId: Long,
    bookingId: Long,
    request: UpsertAttendanceRequest,
  ): OffenderCourseAttendance {
    val courseSchedule = findCourseScheduleOrThrow(courseScheduleId)

    val offenderBooking = findOffenderBookingOrThrow(bookingId)

    val offenderProgramProfile = findOffenderProgramProfileOrThrow(courseSchedule, offenderBooking, bookingId)

    val requestStatus = findEventStatusOrThrow(request.eventStatusCode)

    val attendance = findUpdatableAttendanceOrThrow(courseSchedule, offenderBooking)
      ?: newAttendance(
        courseSchedule,
        offenderBooking,
        offenderProgramProfile,
        requestStatus,
      ).also {
        offenderProgramProfile.endDate?.run {
          if (this.isBefore(courseSchedule.scheduleDate)) {
            if (courseSchedule.courseActivity.prison.id != offenderBooking.location?.id) {
              throw BadDataException("Cannot create an attendance for allocation ${offenderProgramProfile.offenderProgramReferenceId} after its end date of $this with prisoner now in location ${offenderBooking.location?.id}", BadRequestError.PRISONER_MOVED_ALLOCATION_ENDED)
            }
            throw BadDataException("Cannot create an attendance for allocation ${offenderProgramProfile.offenderProgramReferenceId} after its end date of $this")
          }
        }
      }

    return attendance.apply {
      eventDate = request.scheduleDate
      startTime = eventDate.atTime(request.startTime)
      endTime = eventDate.atTime(request.endTime)
      eventStatus = getEventStatus(requestStatus, this.eventStatus)
      attendanceOutcome = request.eventOutcomeCode?.let { findAttendanceOutcomeOrThrow(it) }
      unexcusedAbsence = request.unexcusedAbsence
      bonusPay = request.bonusPay
      pay = request.paid
      authorisedAbsence = request.authorisedAbsence
      commentText = request.comments
      performanceCode = attendanceOutcome?.let { if (it.code == "ATT" && pay == true) "STANDARD" else null }
    }
  }

  private fun newAttendance(
    courseSchedule: CourseSchedule,
    offenderBooking: OffenderBooking,
    offenderProgramProfile: OffenderProgramProfile,
    eventStatus: EventStatus,
  ) = OffenderCourseAttendance(
    offenderBooking = offenderBooking,
    courseSchedule = courseSchedule,
    offenderProgramProfile = offenderProgramProfile,
    courseActivity = courseSchedule.courseActivity,
    eventDate = courseSchedule.scheduleDate,
    startTime = courseSchedule.startTime,
    endTime = courseSchedule.endTime,
    inTime = courseSchedule.startTime,
    outTime = courseSchedule.endTime,
    eventStatus = eventStatus,
    toInternalLocation = courseSchedule.courseActivity.internalLocation,
    prison = courseSchedule.courseActivity.prison,
    program = courseSchedule.courseActivity.program,
    referenceId = courseSchedule.courseScheduleId,
  )

  private fun findAttendanceOutcomeOrThrow(it: String) =
    (
      attendanceOutcomeRepository.findByIdOrNull(AttendanceOutcome.pk(it))
        ?: throw BadDataException("Attendance outcome code $it does not exist")
      )

  private fun findEventStatusOrThrow(eventStatusCode: String) =
    (
      eventStatusRepository.findByIdOrNull(EventStatus.pk(eventStatusCode))
        ?: throw BadDataException(message = "Event status code $eventStatusCode does not exist")
      )

  private fun findAttendance(
    courseSchedule: CourseSchedule,
    offenderBooking: OffenderBooking,
  ): OffenderCourseAttendance? =
    attendanceRepository.findByCourseScheduleAndOffenderBooking(courseSchedule, offenderBooking)

  private fun findUpdatableAttendanceOrThrow(
    courseSchedule: CourseSchedule,
    offenderBooking: OffenderBooking,
  ): OffenderCourseAttendance? =
    findAttendance(courseSchedule, offenderBooking)
      ?.also {
        if (it.isPaid()) {
          throw BadDataException(
            message = "Attendance ${it.eventId} cannot be changed after it has already been paid",
            error = BadRequestError.ATTENDANCE_PAID,
          )
        }
      }

  private fun findOffenderProgramProfileOrThrow(
    courseSchedule: CourseSchedule,
    offenderBooking: OffenderBooking,
    bookingId: Long,
  ) =
    offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(courseSchedule.courseActivity, offenderBooking)
      .maxByOrNull { it.startDate }
      ?: throw BadDataException("Offender program profile for offender booking with id=$bookingId and course activity id=${courseSchedule.courseActivity.courseActivityId} not found")

  private fun findOffenderBookingOrThrow(bookingId: Long) = offenderBookingRepository.findByIdOrNull(bookingId)
    ?: throw BadDataException("Offender booking with id=$bookingId not found")

  private fun findCourseScheduleOrThrow(courseScheduleId: Long) =
    scheduleRepository.findByIdOrNull(courseScheduleId)
      ?: throw BadDataException("Course schedule for courseScheduleId=$courseScheduleId not found")

  private fun getEventStatus(requestStatus: EventStatus, oldStatus: EventStatus) = if (oldStatus.code == "COMP" && requestStatus.code != "CANC") oldStatus else requestStatus

  fun deleteAttendance(eventId: Long) = attendanceRepository.deleteById(eventId)

  fun findPaidAttendancesSummary(prisonId: String, date: LocalDate) =
    attendanceRepository.findBookingPaidAttendanceCountsByPrisonAndDate(prisonId, date)
      .let {
        AttendanceReconciliationResponse(
          prisonId = prisonId,
          date = date,
          bookings = it,
        )
      }
}
