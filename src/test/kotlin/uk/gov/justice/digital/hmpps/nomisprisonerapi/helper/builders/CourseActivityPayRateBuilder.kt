package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import java.math.BigDecimal
import java.time.LocalDate

class CourseActivityPayRateBuilder(
  var iepLevelCode: String = "STD",
  var payBandCode: String = "5",
  var startDate: String = "2022-11-16",
  var endDate: String? = "2022-11-23",
  var halfDayRate: BigDecimal = BigDecimal(3.2),
) {
  fun build(courseActivity: CourseActivity) =
    CourseActivityPayRate(
      courseActivity = courseActivity,
      iepLevelCode = iepLevelCode,
      payBandCode = payBandCode,
      startDate = LocalDate.parse(startDate),
      endDate = endDate?.let { LocalDate.parse(endDate) },
      halfDayRate = halfDayRate,
    )
}
