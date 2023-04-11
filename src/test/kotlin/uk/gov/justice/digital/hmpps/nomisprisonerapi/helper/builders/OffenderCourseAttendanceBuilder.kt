package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class OffenderCourseAttendanceBuilderFactory(
  private val repository: Repository,
) {
  fun builder(
    eventId: Long = 0,
    eventDate: LocalDate = LocalDate.now(),
    startTime: LocalDateTime = eventDate.atTime(9, 0),
    endTime: LocalDateTime = eventDate.atTime(11, 0),
    eventStatusCode: String = "SCH",
    toInternalLocationId: Long? = -8,
    outcomeReasonCode: String? = null,
    paidTransactionId: Long? = null,
  ): OffenderCourseAttendanceBuilder {
    return OffenderCourseAttendanceBuilder(
      repository,
      eventId,
      eventDate,
      startTime,
      endTime,
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
  val eventDate: LocalDate,
  val startTime: LocalDateTime,
  val endTime: LocalDateTime,
  val eventStatusCode: String,
  val toInternalLocationId: Long?,
  val outcomeReasonCode: String?,
  val paidTransactionId: Long?,
) {
  fun build(
    courseSchedule: CourseSchedule,
    offenderProgramProfile: OffenderProgramProfile,
  ): OffenderCourseAttendance =
    OffenderCourseAttendance(
      eventId = eventId,
      offenderBooking = offenderProgramProfile.offenderBooking,
      eventDate = eventDate,
      startTime = startTime,
      endTime = endTime,
      eventStatus = repository.lookupEventStatusCode(eventStatusCode),
      toInternalLocation = toInternalLocationId?.let { repository.lookupAgencyInternalLocation(toInternalLocationId) },
      courseSchedule = courseSchedule,
      attendanceOutcome = outcomeReasonCode?.let { repository.lookupAttendanceOutcomeCode(outcomeReasonCode) },
      offenderProgramProfile = offenderProgramProfile,
      inTime = startTime,
      outTime = endTime,
      courseActivity = courseSchedule.courseActivity,
      prison = courseSchedule.courseActivity.prison,
      program = courseSchedule.courseActivity.program,
      paidTransactionId = paidTransactionId,
    )
}
