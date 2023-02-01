package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import java.math.BigDecimal
import java.time.LocalDate

@Component
class CourseActivityPayRateBuilderFactory(private val repository: Repository) {
  fun builder(
    iepLevelCode: String = "STD",
    payBandCode: String = "5",
    startDate: String = "2022-10-31",
    endDate: String? = null,
    halfDayRate: BigDecimal = BigDecimal(3.2),
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
  val repository: Repository,
  var iepLevelCode: String,
  var payBandCodeId: String,
  var startDate: String,
  var endDate: String?,
  var halfDayRate: BigDecimal,
) {
  fun build(courseActivity: CourseActivity) =
    CourseActivityPayRate(
      id = CourseActivityPayRateId(
        courseActivity = courseActivity,
        iepLevelCode = iepLevelCode,
        payBandCode = payBandCodeId,
        startDate = LocalDate.parse(startDate),
      ),
      payBand = repository.lookupPayBandCode(payBandCodeId),
      iepLevel = repository.lookupIepLevel(iepLevelCode),
      endDate = endDate?.let { LocalDate.parse(endDate) },
      halfDayRate = CourseActivityPayRate.preciseHalfDayRate(halfDayRate),
    )
}
