package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBandId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfilePayBandRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CourseAllocationPayBandDslMarker

@TestDataDslMarker
interface CourseAllocationPayBandDsl

@Component
class CourseAllocationPayBandBuilderRepository(
  private val courseAllocationPayBandRepository: OffenderProgramProfilePayBandRepository,
  private val payBandRepository: ReferenceCodeRepository<PayBand>,
) {
  fun save(payBand: OffenderProgramProfilePayBand) = courseAllocationPayBandRepository.save(payBand)
  fun payBand(payBandCode: String): PayBand = payBandRepository.findByIdOrNull(PayBand.pk(payBandCode))!!
}

@Component
class CourseAllocationPayBandBuilderFactory(private val repository: CourseAllocationPayBandBuilderRepository? = null) {

  fun builder(startDate: String, endDate: String?, payBandCode: String) =
    CourseAllocationPayBandBuilder(repository, startDate, endDate, payBandCode)
}

class CourseAllocationPayBandBuilder(
  private val repository: CourseAllocationPayBandBuilderRepository? = null,
  private val startDate: String,
  private val endDate: String?,
  private val payBandCode: String,
) : CourseAllocationPayBandDsl {

  fun build(courseAllocation: OffenderProgramProfile) =
    OffenderProgramProfilePayBand(
      id = OffenderProgramProfilePayBandId(
        offenderProgramProfile = courseAllocation,
        startDate = LocalDate.parse(startDate),
      ),
      endDate = endDate?.let { LocalDate.parse(endDate) },
      payBand = payBand(payBandCode),
    ).let {
      save(it)
    }

  fun save(payBand: OffenderProgramProfilePayBand) = repository?.save(payBand) ?: payBand
  fun payBand(payBandCode: String) = repository?.payBand(payBandCode)
    ?: PayBand(code = payBandCode, description = payBandCode)
}
