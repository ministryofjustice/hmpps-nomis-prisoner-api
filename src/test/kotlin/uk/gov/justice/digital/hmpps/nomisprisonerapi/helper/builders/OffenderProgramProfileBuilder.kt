package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import java.time.LocalDate

class OffenderProgramProfileBuilder(
  val repository: Repository,
  val startDate: String?,
  val programStatusCode: String,
  val endDate: String?,
  val payBands: MutableList<OffenderProgramProfilePayBandBuilder>,
  val endReasonCode: String?,
  val endComment: String?,
  val attendances: MutableList<OffenderCourseAttendanceBuilder>,
  val offenderBooking: OffenderBooking? = null,
  val courseActivity: CourseActivity? = null,
) : CourseAllocationDsl {
  fun build(offenderBooking: OffenderBooking): OffenderProgramProfile =
    OffenderProgramProfile(
      offenderBooking = offenderBooking,
      program = courseActivity!!.program,
      startDate = LocalDate.parse(startDate),
      programStatus = repository.lookupProgramStatus(programStatusCode),
      courseActivity = courseActivity,
      prison = courseActivity.prison,
      endDate = endDate?.let { LocalDate.parse(endDate) },
      endReason = endReasonCode?.let { repository.lookupProgramEndReason(endReasonCode) },
      endComment = endComment,
    ).apply {
      payBands.addAll(this@OffenderProgramProfileBuilder.payBands.map { it.build(this) })
      offenderCourseAttendances.addAll(this@OffenderProgramProfileBuilder.attendances.map { it.build(this) })
    }

  override fun payBand(
    startDate: String,
    endDate: String?,
    payBandCode: String,
  ) {
    payBands += OffenderProgramProfilePayBandBuilder(repository, startDate, endDate, payBandCode)
  }

  override fun courseAttendance(
    courseSchedule: CourseSchedule,
    eventId: Long,
    eventStatusCode: String,
    toInternalLocationId: Long?,
    outcomeReasonCode: String?,
    paidTransactionId: Long?,
  ) {
    attendances += OffenderCourseAttendanceBuilder(repository, eventId, courseSchedule, eventStatusCode, toInternalLocationId, outcomeReasonCode, paidTransactionId)
  }
}
