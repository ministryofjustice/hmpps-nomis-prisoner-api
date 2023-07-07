package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseScheduleRule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRuleRepository
import java.time.LocalDate

@DslMarker
annotation class CourseScheduleRuleDslMarker

@TestDataDslMarker
interface CourseScheduleRuleDsl

@Component
class CourseScheduleRuleBuilderRepository(private val courseScheduleRuleRepository: CourseScheduleRuleRepository) {
  fun save(rule: CourseScheduleRule) = courseScheduleRuleRepository.save(rule)
}

@Component
class CourseScheduleRuleBuilderFactory(private val repository: CourseScheduleRuleBuilderRepository? = null) {
  fun builder(
    id: Long,
    startTimeHours: Int,
    startTimeMinutes: Int,
    endTimeHours: Int,
    endTimeMinutes: Int,
    monday: Boolean,
    tuesday: Boolean,
    wednesday: Boolean,
    thursday: Boolean,
    friday: Boolean,
    saturday: Boolean,
    sunday: Boolean,
  ): CourseScheduleRuleBuilder =
    CourseScheduleRuleBuilder(
      repository,
      id, startTimeHours, startTimeMinutes, endTimeHours, endTimeMinutes, monday, tuesday, wednesday, thursday, friday, saturday, sunday,
    )
}
class CourseScheduleRuleBuilder(
  val repository: CourseScheduleRuleBuilderRepository? = null,
  val id: Long,
  val startTimeHours: Int,
  val startTimeMinutes: Int,
  val endTimeHours: Int,
  val endTimeMinutes: Int,

  val monday: Boolean,
  val tuesday: Boolean,
  val wednesday: Boolean,
  val thursday: Boolean,
  val friday: Boolean,
  val saturday: Boolean,
  val sunday: Boolean,
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
    ).let {
      save(it)
    }
  }

  fun save(rule: CourseScheduleRule) = repository?.save(rule) ?: rule
}
