package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CourseAllocationDslMarker

@NomisDataDslMarker
interface CourseAllocationDsl {
  @CourseAttendanceDslMarker
  fun courseAttendance(
    courseSchedule: CourseSchedule,
    eventId: Long = 0,
    eventStatusCode: String = "SCH",
    toInternalLocationId: Long? = -3005,
    outcomeReasonCode: String? = null,
    paidTransactionId: Long? = null,
  ): OffenderCourseAttendance

  @CourseAllocationPayBandDslMarker
  fun payBand(
    startDate: String = "2022-10-31",
    endDate: String? = null,
    payBandCode: String = "5",
  ): OffenderProgramProfilePayBand
}

@Component
class CourseAllocationBuilderRepository(
  private val programStatusRepository: ReferenceCodeRepository<OffenderProgramStatus>,
  private val programEndReasonRepository: ReferenceCodeRepository<ProgramServiceEndReason>,
) {
  fun programStatus(code: String): OffenderProgramStatus = programStatusRepository.findByIdOrNull(OffenderProgramStatus.pk(code))!!
  fun programEndReason(code: String): ProgramServiceEndReason = programEndReasonRepository.findByIdOrNull(ProgramServiceEndReason.pk(code))!!
}

@Component
class CourseAllocationBuilderFactory(
  private val repository: CourseAllocationBuilderRepository? = null,
  private val payBandBuilderFactory: CourseAllocationPayBandBuilderFactory = CourseAllocationPayBandBuilderFactory(),
  private val courseAttendanceBuilderFactory: CourseAttendanceBuilderFactory = CourseAttendanceBuilderFactory(),
) {
  fun builder() = CourseAllocationBuilder(
    repository,
    payBandBuilderFactory,
    courseAttendanceBuilderFactory,
  )
}

class CourseAllocationBuilder(
  private val repository: CourseAllocationBuilderRepository? = null,
  private val payBandBuilderFactory: CourseAllocationPayBandBuilderFactory,
  private val courseAttendanceBuilderFactory: CourseAttendanceBuilderFactory,
) : CourseAllocationDsl {

  private lateinit var courseAllocation: OffenderProgramProfile

  fun build(
    offenderBooking: OffenderBooking,
    startDate: String?,
    programStatusCode: String,
    endDate: String?,
    endReasonCode: String?,
    endComment: String?,
    courseActivity: CourseActivity,
  ) =
    OffenderProgramProfile(
      offenderBooking = offenderBooking,
      program = courseActivity.program,
      startDate = LocalDate.parse(startDate),
      programStatus = programStatus(programStatusCode),
      courseActivity = courseActivity,
      prison = courseActivity.prison,
      endDate = endDate?.let { LocalDate.parse(endDate) },
      endReason = endReasonCode?.let { programEndReason(endReasonCode) },
      endComment = endComment,
    )
      .also { courseAllocation = it }

  override fun payBand(
    startDate: String,
    endDate: String?,
    payBandCode: String,
  ): OffenderProgramProfilePayBand =
    payBandBuilderFactory.builder()
      .build(courseAllocation, startDate, endDate, payBandCode)
      .also { courseAllocation.payBands += it }

  override fun courseAttendance(
    courseSchedule: CourseSchedule,
    eventId: Long,
    eventStatusCode: String,
    toInternalLocationId: Long?,
    outcomeReasonCode: String?,
    paidTransactionId: Long?,
  ) =
    courseAttendanceBuilderFactory.builder().build(
      courseAllocation,
      courseSchedule,
      eventId,
      eventStatusCode,
      toInternalLocationId,
      outcomeReasonCode,
      paidTransactionId,
    )
      .also { courseAllocation.offenderCourseAttendances += it }

  private fun programStatus(programStatusCode: String) = repository?.programStatus(programStatusCode)
    ?: OffenderProgramStatus(code = programStatusCode, description = programStatusCode)

  private fun programEndReason(endReasonCode: String) = repository?.programEndReason(endReasonCode)
    ?: ProgramServiceEndReason(code = endReasonCode, description = endReasonCode)
}
