package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AttendanceOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourseAttendanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class CourseAttendanceDslMarker

@TestDataDslMarker
interface CourseAttendanceDsl

@Component
class CourseAttendanceBuilderRepository(
  private val offenderCourseAttendanceRepository: OffenderCourseAttendanceRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val attendanceOutcomeRepository: ReferenceCodeRepository<AttendanceOutcome>,
) {
  fun save(courseAttendance: OffenderCourseAttendance) = offenderCourseAttendanceRepository.save(courseAttendance)
  fun agencyInternalLocation(locationId: Long) = agencyInternalLocationRepository.findByIdOrNull(locationId)
  fun eventStatus(code: String) = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun attendanceOutcome(code: String) = attendanceOutcomeRepository.findByIdOrNull(AttendanceOutcome.pk(code))!!
}

@Component
class CourseAttendanceBuilderFactory(
  private val repository: CourseAttendanceBuilderRepository,
) {
  fun builder(
    courseSchedule: CourseSchedule,
    eventId: Long,
    eventStatusCode: String,
    toInternalLocationId: Long?,
    outcomeReasonCode: String?,
    paidTransactionId: Long?,
  ) = CourseAttendanceBuilder(repository, courseSchedule, eventId, eventStatusCode, toInternalLocationId, outcomeReasonCode, paidTransactionId)
}

class CourseAttendanceBuilder(
  private val repository: CourseAttendanceBuilderRepository,
  private val courseSchedule: CourseSchedule,
  private val eventId: Long,
  private val eventStatusCode: String,
  private val toInternalLocationId: Long?,
  private val outcomeReasonCode: String?,
  private val paidTransactionId: Long?,
) : CourseAttendanceDsl {

  fun build(
    offenderProgramProfile: OffenderProgramProfile,
  ): OffenderCourseAttendance =
    OffenderCourseAttendance(
      eventId = eventId,
      offenderBooking = offenderProgramProfile.offenderBooking,
      eventDate = courseSchedule.scheduleDate,
      startTime = courseSchedule.startTime,
      endTime = courseSchedule.endTime,
      eventStatus = repository.eventStatus(eventStatusCode),
      toInternalLocation = toInternalLocationId?.let { repository.agencyInternalLocation(toInternalLocationId) },
      courseSchedule = courseSchedule,
      attendanceOutcome = outcomeReasonCode?.let { repository.attendanceOutcome(outcomeReasonCode) },
      offenderProgramProfile = offenderProgramProfile,
      inTime = courseSchedule.startTime,
      outTime = courseSchedule.endTime,
      courseActivity = courseSchedule.courseActivity,
      prison = courseSchedule.courseActivity.prison,
      program = courseSchedule.courseActivity.program,
      paidTransactionId = paidTransactionId,
    ).let {
      repository.save(it)
    }
}
