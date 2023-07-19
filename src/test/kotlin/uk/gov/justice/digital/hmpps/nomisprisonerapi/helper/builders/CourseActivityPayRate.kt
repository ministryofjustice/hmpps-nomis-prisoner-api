package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@DslMarker
annotation class CourseActivityPayRateDslMarker

@NomisDataDslMarker
interface CourseActivityPayRateDsl

@Component
class CourseActivityPayRateBuilderRepository(
  val payBandRepository: ReferenceCodeRepository<PayBand>,
  val iepLevelRepository: ReferenceCodeRepository<IEPLevel>,
) {
  fun lookupPayBandCode(code: String): PayBand = payBandRepository.findByIdOrNull(PayBand.pk(code))!!
  fun lookupIepLevel(code: String): IEPLevel =
    iepLevelRepository.findByIdOrNull(ReferenceCode.Pk(IEPLevel.IEP_LEVEL, code))!!
}

@Component
class CourseActivityPayRateBuilderFactory(private val repository: CourseActivityPayRateBuilderRepository? = null) {
  fun builder(): CourseActivityPayRateBuilder = CourseActivityPayRateBuilder(repository)
}

class CourseActivityPayRateBuilder(val repository: CourseActivityPayRateBuilderRepository?) : CourseActivityPayRateDsl {
  fun build(
    courseActivity: CourseActivity,
    iepLevelCode: String,
    payBandCode: String,
    startDate: String,
    endDate: String?,
    halfDayRate: Double,
  ) =
    CourseActivityPayRate(
      id = CourseActivityPayRateId(
        courseActivity = courseActivity,
        iepLevelCode = iepLevelCode,
        payBandCode = payBandCode,
        startDate = LocalDate.parse(startDate),
      ),
      payBand = lookupPayBandCode(payBandCode),
      iepLevel = lookupIepLevel(iepLevelCode),
      endDate = endDate?.let { LocalDate.parse(endDate) },
      halfDayRate = BigDecimal(halfDayRate).setScale(3, RoundingMode.HALF_UP),
    )

  private fun lookupPayBandCode(payBandCode: String) = repository?.lookupPayBandCode(payBandCode)
    ?: PayBand(code = payBandCode, description = payBandCode)

  private fun lookupIepLevel(iepLevelCode: String) = repository?.lookupIepLevel(iepLevelCode)
    ?: IEPLevel(code = iepLevelCode, description = iepLevelCode)
}
