package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CourseAllocationDslMarker

@TestDataDslMarker
interface CourseAllocationDsl {

  @CourseAllocationPayBandDslMarker
  fun payBand(
    startDate: String = "2022-10-31",
    endDate: String? = null,
    payBandCode: String = "5",
  )

  @CourseAttendanceDslMarker
  fun courseAttendance(
    courseSchedule: CourseSchedule,
    eventId: Long = 0,
    eventStatusCode: String = "SCH",
    toInternalLocationId: Long? = -8,
    outcomeReasonCode: String? = null,
    paidTransactionId: Long? = null,
  )
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
  private val repository: CourseAllocationBuilderRepository,
  private val payBandBuilderFactory: CourseAllocationPayBandBuilderFactory,
  private val courseAttendanceBuilderFactory: CourseAttendanceBuilderFactory,
) {

  fun builder(
    startDate: String?,
    programStatusCode: String,
    endDate: String?,
    endReasonCode: String?,
    endComment: String?,
    courseActivity: CourseActivity,
  ) = CourseAllocationBuilder(repository, payBandBuilderFactory, courseAttendanceBuilderFactory, startDate, programStatusCode, endDate, endReasonCode, endComment, courseActivity)
}

class CourseAllocationBuilder(
  private val repository: CourseAllocationBuilderRepository,
  private val payBandBuilderFactory: CourseAllocationPayBandBuilderFactory,
  private val courseAttendanceBuilderFactory: CourseAttendanceBuilderFactory,
  private val startDate: String?,
  private val programStatusCode: String,
  private val endDate: String?,
  private val endReasonCode: String?,
  private val endComment: String?,
  private val courseActivity: CourseActivity,
) : CourseAllocationDsl {

  private val payBandBuilders: MutableList<CourseAllocationPayBandBuilder> = mutableListOf()
  private val attendanceBuilders: MutableList<CourseAttendanceBuilder> = mutableListOf()

  fun build(offenderBooking: OffenderBooking): OffenderProgramProfile =
    OffenderProgramProfile(
      offenderBooking = offenderBooking,
      program = courseActivity.program,
      startDate = LocalDate.parse(startDate),
      programStatus = repository.programStatus(programStatusCode),
      courseActivity = courseActivity,
      prison = courseActivity.prison,
      endDate = endDate?.let { LocalDate.parse(endDate) },
      endReason = endReasonCode?.let { repository.programEndReason(endReasonCode) },
      endComment = endComment,
    )
      .let { repository.save(it) }
      .apply {
        payBands.addAll(payBandBuilders.map { it.build(this) })
        offenderCourseAttendances.addAll(attendanceBuilders.map { it.build(this) })
      }

  override fun payBand(
    startDate: String,
    endDate: String?,
    payBandCode: String,
  ) {
    payBandBuilders += payBandBuilderFactory.builder(startDate, endDate, payBandCode)
  }

  override fun courseAttendance(
    courseSchedule: CourseSchedule,
    eventId: Long,
    eventStatusCode: String,
    toInternalLocationId: Long?,
    outcomeReasonCode: String?,
    paidTransactionId: Long?,
  ) {
    attendanceBuilders += courseAttendanceBuilderFactory.builder(
      courseSchedule,
      eventId,
      eventStatusCode,
      toInternalLocationId,
      outcomeReasonCode,
      paidTransactionId,
    )
  }
}
