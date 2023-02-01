package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import java.time.LocalDate

@Component
class OffenderProgramProfileBuilderFactory(
  private val repository: Repository,
  private val offenderProgramProfilePayBandBuilderFactory: OffenderProgramProfilePayBandBuilderFactory
) {
  fun builder(
    programId: Long = 20,
    startDate: String? = "2022-10-31",
    programStatusCode: String = "ALLOC",
    endDate: String? = null,
    payBands: List<OffenderProgramProfilePayBandBuilder> = listOf(offenderProgramProfilePayBandBuilderFactory.builder()),
    endReasonCode: String? = null,
    endComment: String? = null,
  ): OffenderProgramProfileBuilder =
    OffenderProgramProfileBuilder(
      repository,
      programId,
      startDate,
      programStatusCode,
      endDate,
      payBands,
      endReasonCode,
      endComment,
    )
}

class OffenderProgramProfileBuilder(
  val repository: Repository,
  val programId: Long,
  val startDate: String?,
  val programStatusCode: String,
  val endDate: String?,
  val payBands: List<OffenderProgramProfilePayBandBuilder>,
  val endReasonCode: String?,
  val endComment: String?,
) {
  fun build(offenderBooking: OffenderBooking, courseActivity: CourseActivity): OffenderProgramProfile =
    OffenderProgramProfile(
      offenderBooking = offenderBooking,
      program = repository.lookupProgramService(programId),
      startDate = startDate?.let { LocalDate.parse(startDate) },
      programStatus = repository.lookupProgramStatus(programStatusCode),
      courseActivity = courseActivity,
      prison = courseActivity.prison,
      endDate = endDate?.let { LocalDate.parse(endDate) },
      endReason = endReasonCode?.let { repository.lookupProgramEndReason(endReasonCode) },
      endComment = endComment,
    ).apply {
      payBands.addAll(this@OffenderProgramProfileBuilder.payBands.map { it.build(this) })
    }
}
