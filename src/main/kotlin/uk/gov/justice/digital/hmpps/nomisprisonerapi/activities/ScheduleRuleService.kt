package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseScheduleRule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Service
class ScheduleRuleService {

  fun mapRules(dto: CreateActivityRequest, courseActivity: CourseActivity) =
    dto.scheduleRules.map {
      validateRequest(it)

      // Prod data usually uses 1st day of the scheduleStartDate month for the 'date' part
      val monthStart = courseActivity.scheduleStartDate!!.with(TemporalAdjusters.firstDayOfMonth())

      CourseScheduleRule(
        courseActivity = courseActivity,
        startTime = LocalDateTime.of(monthStart, it.startTime),
        endTime = LocalDateTime.of(monthStart, it.endTime),
        slotCategory = SlotCategory.of(it.startTime),
        monday = it.monday,
        tuesday = it.tuesday,
        wednesday = it.wednesday,
        thursday = it.thursday,
        friday = it.friday,
        saturday = it.saturday,
        sunday = it.sunday,
      )
    }

  private fun validateRequest(request: ScheduleRuleRequest) {
    if (request.endTime < request.startTime) {
      throw BadDataException("Schedule rule has times out of order - ${request.startTime} to ${request.endTime}")
    }
  }

  fun buildNewRules(requestedRules: List<ScheduleRuleRequest>, existingActivity: CourseActivity): List<CourseScheduleRule> {
    val monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
    return requestedRules.map {
      validateRequest(it)
      CourseScheduleRule(
        id = findExistingRuleId(it, existingActivity.courseScheduleRules),
        courseActivity = existingActivity,
        startTime = LocalDateTime.of(monthStart, it.startTime),
        endTime = LocalDateTime.of(monthStart, it.endTime),
        slotCategory = SlotCategory.of(it.startTime),
        monday = it.monday,
        tuesday = it.tuesday,
        wednesday = it.wednesday,
        thursday = it.thursday,
        friday = it.friday,
        saturday = it.saturday,
        sunday = it.sunday,
      )
    }
  }

  private fun findExistingRuleId(requestedRule: ScheduleRuleRequest, existingRules: List<CourseScheduleRule>): Long =
    existingRules.find {
      it.startTime.toLocalTime() == requestedRule.startTime &&
        it.endTime?.toLocalTime() == requestedRule.endTime &&
        it.monday == requestedRule.monday &&
        it.tuesday == requestedRule.tuesday &&
        it.wednesday == requestedRule.wednesday &&
        it.thursday == requestedRule.thursday &&
        it.friday == requestedRule.friday &&
        it.saturday == requestedRule.saturday &&
        it.sunday == requestedRule.sunday
    }
      ?.id
      ?: let { 0 }
}
