package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.ScheduleRuleRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.ScheduleRulesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseScheduleRule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

@Service
class ScheduleRuleService {

  fun mapRules(dto: CreateActivityRequest, courseActivity: CourseActivity): List<CourseScheduleRule> {
    val monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
    return dto.scheduleRules.map {
      validateRequest(it)
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
  }

  fun mapRules(scheduleRules: List<CourseScheduleRule>): List<ScheduleRulesResponse> =
    scheduleRules.map {
      ScheduleRulesResponse(
        startTime = it.startTime.toLocalTime(),
        endTime = it.endTime.toLocalTime(),
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
    return requestedRules.map { request ->
      validateRequest(request)
      findExistingRule(request, existingActivity.courseScheduleRules)
        ?: with(request) {
          CourseScheduleRule(
            id = 0,
            courseActivity = existingActivity,
            startTime = LocalDateTime.of(monthStart, startTime),
            endTime = LocalDateTime.of(monthStart, endTime),
            slotCategory = SlotCategory.of(startTime),
            monday = monday,
            tuesday = tuesday,
            wednesday = wednesday,
            thursday = thursday,
            friday = friday,
            saturday = saturday,
            sunday = sunday,
          )
        }
    }
  }

  private fun findExistingRule(requestedRule: ScheduleRuleRequest, existingRules: List<CourseScheduleRule>): CourseScheduleRule? =
    existingRules.find {
      it.startTime.toLocalTime() == requestedRule.startTime &&
        it.endTime.toLocalTime() == requestedRule.endTime &&
        it.monday == requestedRule.monday &&
        it.tuesday == requestedRule.tuesday &&
        it.wednesday == requestedRule.wednesday &&
        it.thursday == requestedRule.thursday &&
        it.friday == requestedRule.friday &&
        it.saturday == requestedRule.saturday &&
        it.sunday == requestedRule.sunday
    }

  fun buildUpdateTelemetry(savedRules: List<CourseScheduleRule>, newRules: List<CourseScheduleRule>): Map<String, String> {
    val removedRuleIds = savedRules.map { it.id } - newRules.map { it.id }.toSet()
    val createdRuleIds = newRules.map { it.id } - savedRules.map { it.id }.toSet()
    val telemetry = mutableMapOf<String, String>()
    if (removedRuleIds.isNotEmpty()) {
      telemetry["removed-courseScheduleRuleIds"] = removedRuleIds.toString()
    }
    if (createdRuleIds.isNotEmpty()) {
      telemetry["created-courseScheduleRuleIds"] = createdRuleIds.toString()
    }
    return telemetry
  }
}
