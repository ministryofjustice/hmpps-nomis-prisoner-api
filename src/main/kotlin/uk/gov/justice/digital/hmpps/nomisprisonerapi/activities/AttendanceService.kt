package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAttendanceRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateAttendanceResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AttendanceOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourseAttendanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

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

  fun createAttendance(
    scheduleId: Long,
    bookingId: Long,
    createAttendanceRequest: CreateAttendanceRequest,
  ): CreateAttendanceResponse =
    attendanceRepository.save(createAttendanceRequest.toOffenderCourseAttendance(scheduleId, bookingId))
      .let { CreateAttendanceResponse(it.eventId, it.courseSchedule!!.courseScheduleId) }

  private fun CreateAttendanceRequest.toOffenderCourseAttendance(courseActivityId: Long, bookingId: Long): OffenderCourseAttendance {
    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId not found")

    val courseSchedules = scheduleRepository.findByCourseActivityAndScheduleDate(courseActivity, scheduleDate)
      .filter { it.startTime == scheduleDate.atTime(startTime) && it.endTime == scheduleDate.atTime(endTime) }

    val courseSchedule = when (courseSchedules.size) {
      1 -> courseSchedules.first()
      0 -> throw NotFoundException("Course schedule for activity=$courseActivityId, date=$scheduleDate, start time=$startTime and end time=$endTime not found")
      else -> throw IllegalStateException("Found multiple course schedules for activity=$courseActivityId, date=$scheduleDate, start time=$startTime and end time=$endTime")
    }

    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Offender booking with id=$bookingId not found")

    val offenderProgramProfile = offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(courseSchedule.courseActivity, offenderBooking)
      ?: throw NotFoundException("Offender program profile for offender booking with id=$bookingId and course activity id=${courseSchedule.courseActivity.courseActivityId} not found")

    attendanceRepository.findByCourseScheduleAndOffenderBooking(courseSchedule, offenderBooking)
      .filter { listOf("SCH", "COMP").contains(it.eventStatus.code) }
      .takeIf { it.isNotEmpty() }
      ?.run { throw ConflictException("Offender course attendance already exists with eventId=${this.first().eventId} and eventStatus=${this.first().eventStatus.code}") }

    val eventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(this.eventStatusCode))
      ?: throw BadDataException("Event status code ${this.eventStatusCode} does not exist")

    val attendanceOutcome = this.eventOutcomeCode?.let {
      attendanceOutcomeRepository.findByIdOrNull(AttendanceOutcome.pk(it))
        ?: throw BadDataException("Attendance outcome code $it does not exist")
    }

    return OffenderCourseAttendance(
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
      attendanceOutcome = attendanceOutcome,
      prison = courseSchedule.courseActivity.prison,
      unexcusedAbsence = this.unexcusedAbsence,
      program = courseSchedule.courseActivity.program,
      bonusPay = this.bonusPay,
      paid = this.paid,
      authorisedAbsence = this.authorisedAbsence,
      commentText = this.comments,
    )
  }
}
