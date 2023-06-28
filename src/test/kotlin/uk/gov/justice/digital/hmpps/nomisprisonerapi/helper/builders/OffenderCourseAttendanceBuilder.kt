package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile

@Component
class OffenderCourseAttendanceBuilderFactory(
  private val repository: Repository,
) {
  fun builder(
    eventId: Long = 0,
    courseSchedule: CourseSchedule,
    eventStatusCode: String = "SCH",
    toInternalLocationId: Long? = -8,
    outcomeReasonCode: String? = null,
    paidTransactionId: Long? = null,
  ): OffenderCourseAttendanceBuilder {
    return OffenderCourseAttendanceBuilder(
      repository,
      eventId,
      courseSchedule,
      eventStatusCode,
      toInternalLocationId,
      outcomeReasonCode,
      paidTransactionId,
    )
  }
}

class OffenderCourseAttendanceBuilder(
  val repository: Repository,
  val eventId: Long,
  val courseSchedule: CourseSchedule,
  val eventStatusCode: String,
  val toInternalLocationId: Long?,
  val outcomeReasonCode: String?,
  val paidTransactionId: Long?,
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
      eventStatus = repository.lookupEventStatusCode(eventStatusCode),
      toInternalLocation = toInternalLocationId?.let { repository.lookupAgencyInternalLocation(toInternalLocationId) },
      courseSchedule = courseSchedule,
      attendanceOutcome = outcomeReasonCode?.let { repository.lookupAttendanceOutcomeCode(outcomeReasonCode) },
      offenderProgramProfile = offenderProgramProfile,
      inTime = courseSchedule.startTime,
      outTime = courseSchedule.endTime,
      courseActivity = courseSchedule.courseActivity,
      prison = courseSchedule.courseActivity.prison,
      program = courseSchedule.courseActivity.program,
      paidTransactionId = paidTransactionId,
    )
}
