package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Component
class CourseActivityPayRateBuilderFactory(private val repository: Repository? = null) {
  fun builder(
    iepLevelCode: String = "STD",
    payBandCode: String = "5",
    startDate: String = "2022-10-31",
    endDate: String? = null,
    halfDayRate: Double = 3.2,
  ): CourseActivityPayRateBuilder =
    CourseActivityPayRateBuilder(
      repository,
      iepLevelCode,
      payBandCode,
      startDate,
      endDate,
      halfDayRate,
    )
}
class CourseActivityPayRateBuilder(
  val repository: Repository?,
  var iepLevelCode: String,
  var payBandCode: String,
  var startDate: String,
  var endDate: String?,
  var halfDayRate: Double,
) {
  fun build(courseActivity: CourseActivity) =
    repository?.let {
      CourseActivityPayRate(
        id = CourseActivityPayRateId(
          courseActivity = courseActivity,
          iepLevelCode = iepLevelCode,
          payBandCode = payBandCode,
          startDate = LocalDate.parse(startDate),
        ),
        payBand = repository.lookupPayBandCode(payBandCode),
        iepLevel = repository.lookupIepLevel(iepLevelCode),
        endDate = endDate?.let { LocalDate.parse(endDate) },
        halfDayRate = BigDecimal(halfDayRate).setScale(3, RoundingMode.HALF_UP),
      )
    }
      ?: throw IllegalStateException("No repository - is this a unit test? Try create() instead.")

  fun create(courseActivity: CourseActivity) =
    CourseActivityPayRate(
      id = CourseActivityPayRateId(
        courseActivity = courseActivity,
        iepLevelCode = iepLevelCode,
        payBandCode = payBandCode,
        startDate = LocalDate.parse(startDate),
      ),
      payBand = payBand(payBandCode),
      iepLevel = iepLevel(iepLevelCode),
      endDate = endDate?.let { LocalDate.parse(endDate) },
      halfDayRate = BigDecimal(halfDayRate).setScale(3, RoundingMode.HALF_UP),
    )

  private fun payBand(code: String) = PayBand(code = code, description = code)

  private fun iepLevel(code: String) = IEPLevel(code = code, description = code)
}
