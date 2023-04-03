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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourseAttendanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@Service
@Transactional
class AttendanceService(
  private val attendanceRepository: OffenderCourseAttendanceRepository,
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
      .let { CreateAttendanceResponse(it.eventId) }

  private fun CreateAttendanceRequest.toOffenderCourseAttendance(scheduleId: Long, bookingId: Long): OffenderCourseAttendance {
    val courseSchedule = scheduleRepository.findByIdOrNull(scheduleId)
      ?: run { throw NotFoundException("Course schedule with id=$scheduleId not found") }

    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: run { throw NotFoundException("Offender booking with id=$bookingId not found") }

    val offenderProgramProfile = offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(courseSchedule.courseActivity, offenderBooking)
      ?: run { throw NotFoundException("Offender program profile for offender booking with id=$bookingId and course activity id=${courseSchedule.courseActivity.courseActivityId} not found") }

    attendanceRepository.findByCourseScheduleAndOffenderBooking(courseSchedule, offenderBooking)
      .filter { listOf("SCH", "COMP").contains(it.eventStatus.code) }
      .takeIf { it.isNotEmpty() }
      ?.run { throw ConflictException("Offender course attendance already exists with eventId=${this.first().eventId} and eventStatus=${this.first().eventStatus.code}") }

    val eventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(this.eventStatusCode))
      ?: run { throw BadDataException("Event status code ${this.eventStatusCode} does not exist") }

    val attendanceOutcome = this.eventOutcomeCode?.let {
      attendanceOutcomeRepository.findByIdOrNull(AttendanceOutcome.pk(it))
        ?: run { throw BadDataException("Attendance outcome code $it does not exist") }
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
