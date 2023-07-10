package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CourseAllocationDslMarker

@NomisDataDslMarker
interface CourseAllocationDsl : CourseAttendanceDslApi, CourseAllocationPayBandDslApi

interface CourseAllocationDslApi {
  @CourseAllocationDslMarker
  fun courseAllocation(
    courseActivity: CourseActivity,
    startDate: String? = "2022-10-31",
    programStatusCode: String = "ALLOC",
    endDate: String? = null,
    endReasonCode: String? = null,
    endComment: String? = null,
    dsl: CourseAllocationDsl.() -> Unit = { payBand() },
  ): OffenderProgramProfile
}

@Component
class CourseAllocationBuilderRepository(
  private val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  private val programStatusRepository: ReferenceCodeRepository<OffenderProgramStatus>,
  private val programEndReasonRepository: ReferenceCodeRepository<ProgramServiceEndReason>,
) {
  fun save(courseAllocation: OffenderProgramProfile) = offenderProgramProfileRepository.save(courseAllocation)
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
      .let { save(it) }
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

  private fun save(offenderProgramProfile: OffenderProgramProfile) = repository?.save(offenderProgramProfile) ?: offenderProgramProfile

  private fun programStatus(programStatusCode: String) = repository?.programStatus(programStatusCode)
    ?: OffenderProgramStatus(code = programStatusCode, description = programStatusCode)

  private fun programEndReason(endReasonCode: String) = repository?.programEndReason(endReasonCode)
    ?: ProgramServiceEndReason(code = endReasonCode, description = endReasonCode)
}
