package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAttendanceResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AttendanceOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourseAttendanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional
class AttendanceService(
  private val attendanceRepository: OffenderCourseAttendanceRepository,
  private val activityRepository: CourseActivityRepository,
  private val scheduleRepository: CourseScheduleRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val attendanceOutcomeRepository: ReferenceCodeRepository<AttendanceOutcome>,
) {

  fun upsertAttendance(
    scheduleId: Long,
    bookingId: Long,
    upsertAttendanceRequest: UpsertAttendanceRequest,
  ): UpsertAttendanceResponse {
    val attendance = toOffenderCourseAttendance(scheduleId, bookingId, upsertAttendanceRequest)
    val created = attendance.eventId == 0L
    return attendanceRepository.save(attendance)
      .let { UpsertAttendanceResponse(it.eventId, it.courseSchedule!!.courseScheduleId, created) }
  }

  private fun toOffenderCourseAttendance(courseActivityId: Long, bookingId: Long, request: UpsertAttendanceRequest): OffenderCourseAttendance {
    val courseActivity = findCourseActivityOrThrow(courseActivityId)

    val courseSchedule = with(request) {
      findCourseScheduleOrThrow(courseActivity, courseActivityId, scheduleDate, startTime, endTime)
    }

    val offenderBooking = findOffenderBookingOrThrow(bookingId)

    val offenderProgramProfile = findOffenderProgramProfileOrThrow(courseSchedule, offenderBooking, bookingId)

    val status = findEventStatusOrThrow(request.eventStatusCode)

    val attendance = findAttendance(courseSchedule, offenderBooking)
      ?: newAttendance(
        courseSchedule,
        offenderBooking,
        offenderProgramProfile,
        status,
      )

    return attendance.apply {
      eventStatus = status
      attendanceOutcome = request.eventOutcomeCode?.let { findAttendanceOutcomeOrThrow(it) }
      unexcusedAbsence = request.unexcusedAbsence
      bonusPay = request.bonusPay
      paid = request.paid
      authorisedAbsence = request.authorisedAbsence
      commentText = request.comments
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
  )

  private fun findAttendanceOutcomeOrThrow(it: String) =
    (
      attendanceOutcomeRepository.findByIdOrNull(AttendanceOutcome.pk(it))
        ?: throw BadDataException("Attendance outcome code $it does not exist")
      )

  private fun findEventStatusOrThrow(eventStatusCode: String) =
    (
      eventStatusRepository.findByIdOrNull(EventStatus.pk(eventStatusCode))
        ?: throw BadDataException("Event status code $eventStatusCode does not exist")
      )

  private fun findAttendance(
    courseSchedule: CourseSchedule,
    offenderBooking: OffenderBooking,
  ): OffenderCourseAttendance? =
    attendanceRepository.findByCourseScheduleAndOffenderBooking(courseSchedule, offenderBooking)

  private fun findOffenderProgramProfileOrThrow(
    courseSchedule: CourseSchedule,
    offenderBooking: OffenderBooking,
    bookingId: Long,
  ) =
    offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(
      courseSchedule.courseActivity,
      offenderBooking,
    )
      ?: throw NotFoundException("Offender program profile for offender booking with id=$bookingId and course activity id=${courseSchedule.courseActivity.courseActivityId} not found")

  private fun findOffenderBookingOrThrow(bookingId: Long) = offenderBookingRepository.findByIdOrNull(bookingId)
    ?: throw NotFoundException("Offender booking with id=$bookingId not found")

  private fun findCourseScheduleOrThrow(
    courseActivity: CourseActivity,
    courseActivityId: Long,
    scheduleDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
  ) =
    scheduleRepository.findByCourseActivityAndScheduleDateAndStartTimeAndEndTime(
      courseActivity,
      scheduleDate,
      scheduleDate.atTime(startTime),
      scheduleDate.atTime(endTime),
    )
      ?: throw NotFoundException("Course schedule for activity=$courseActivityId, date=$scheduleDate, start time=$startTime and end time=$endTime not found")

  private fun findCourseActivityOrThrow(courseActivityId: Long) = (
    activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId not found")
    )
}
