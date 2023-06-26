package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseScheduleRule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate

class CourseScheduleRuleBuilder(
  val id: Long = 0,
  val startTimeHours: Int = 9,
  val startTimeMinutes: Int = 30,
  val endTimeHours: Int = 12,
  val endTimeMinutes: Int = 30,

  val monday: Boolean = true,
  val tuesday: Boolean = true,
  val wednesday: Boolean = true,
  val thursday: Boolean = true,
  val friday: Boolean = true,
  val saturday: Boolean = false,
  val sunday: Boolean = false,
) : CourseScheduleRuleDsl {
  fun build(courseActivity: CourseActivity): CourseScheduleRule {
    val date = LocalDate.now().withDayOfMonth(1)
    return CourseScheduleRule(
      id = id,
      courseActivity = courseActivity,
      startTime = date.atTime(startTimeHours, startTimeMinutes),
      endTime = date.atTime(endTimeHours, endTimeMinutes),
      monday = monday,
      tuesday = tuesday,
      wednesday = wednesday,
      thursday = thursday,
      friday = friday,
      saturday = saturday,
      sunday = sunday,
      slotCategory = SlotCategory.of(date.atTime(startTimeHours, startTimeMinutes).toLocalTime()),
    )
  }
}
