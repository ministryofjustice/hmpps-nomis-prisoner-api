package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.ScheduleRuleRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseScheduleRuleBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseScheduleRule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate
import java.time.LocalTime

class ScheduleRuleServiceTest {

  private val scheduleRuleService = ScheduleRuleService()

  @Nested
  inner class CreateScheduleRules {

    private val createActivityRequest = CreateActivityRequest(
      code = "ANY",
      startDate = LocalDate.of(2022, 10, 31),
      endDate = null,
      prisonId = "ANY",
      capacity = 10,
      payRates = listOf(),
      description = "any",
      programCode = "ANY",
      payPerSession = PayPerSession.H,
      internalLocationId = null,
      excludeBankHolidays = true,
    )
    private val courseActivity = CourseActivityBuilderFactory().builder(startDate = "2022-10-31").create()
    private val createScheduleRuleRequest = ScheduleRuleRequest(
      startTime = LocalTime.parse("09:00"),
      endTime = LocalTime.parse("12:00"),
      monday = true,
      wednesday = true,
      saturday = true,
      sunday = true,
    )
    private val monthStart = LocalDate.now().withDayOfMonth(1).toString()

    @Nested
    inner class Validation {
      @Test
      fun `should throw if start is after end time`() {
        val request =
          createActivityRequest.copy(scheduleRules = listOf(createScheduleRuleRequest.copy(endTime = LocalTime.parse("05:00"))))

        assertThatThrownBy {
          scheduleRuleService.mapRules(request, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule rule has times out of order - 09:00 to 05:00")
      }
    }

    @Nested
    inner class Create {
      @Test
      fun `should do nothing if no rules`() {
        val request = createActivityRequest.copy(scheduleRules = listOf())

        assertThat(scheduleRuleService.mapRules(request, courseActivity)).isEmpty()
      }

      @Test
      fun `should save a single schedule`() {
        val request = createActivityRequest.copy(scheduleRules = listOf(createScheduleRuleRequest))

        val newScheduleRules = scheduleRuleService.mapRules(request, courseActivity)

        assertThat(newScheduleRules.size).isEqualTo(1)
        with(newScheduleRules[0]) {
          assertThat(monday).isTrue
          assertThat(tuesday).isFalse
          assertThat(wednesday).isTrue
          assertThat(thursday).isFalse
          assertThat(friday).isFalse
          assertThat(saturday).isTrue
          assertThat(sunday).isTrue
          assertThat(startTime).isEqualTo("${monthStart}T09:00")
          assertThat(endTime).isEqualTo("${monthStart}T12:00")
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
      }

      @Test
      fun `should save multiple schedules`() {
        val request = createActivityRequest.copy(
          scheduleRules = listOf(
            createScheduleRuleRequest,
            createScheduleRuleRequest.copy(startTime = LocalTime.parse("08:00")),
          ),
        )

        val newScheduleRules = scheduleRuleService.mapRules(request, courseActivity)

        assertThat(newScheduleRules.size).isEqualTo(2)
        with(newScheduleRules[0]) {
          assertThat(startTime).isEqualTo("${monthStart}T09:00")
        }
        with(newScheduleRules[1]) {
          assertThat(startTime).isEqualTo("${monthStart}T08:00")
        }
      }
    }
  }

  @Nested
  inner class BuildNewRules {

    // by default the new rule requested is the same as existing rule created by the builder
    private val scheduleRuleRequest = ScheduleRuleRequest(
      startTime = LocalTime.parse("09:30"),
      endTime = LocalTime.parse("12:30"),
      monday = true,
      tuesday = true,
      wednesday = true,
      thursday = true,
      friday = true,
    )

    private val monthStart = LocalDate.now().withDayOfMonth(1).toString()

    @Nested
    inner class Validation {

      @Test
      fun `should throw if start after end time`() {
        val existingActivity = CourseActivityBuilderFactory().builder(courseScheduleRules = listOf()).create()
        val requestedRules =
          listOf(scheduleRuleRequest.copy(startTime = LocalTime.of(12, 0), endTime = LocalTime.of(11, 59)))

        assertThatThrownBy {
          scheduleRuleService.buildNewRules(requestedRules, existingActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule rule has times out of order - 12:00 to 11:59")
      }
    }

    @Nested
    inner class Update {
      @Test
      fun `should do nothing if no rules`() {
        val existingActivity = CourseActivityBuilderFactory().builder(courseScheduleRules = listOf()).create()
        val requestedRules = listOf<ScheduleRuleRequest>()

        val newRules = scheduleRuleService.buildNewRules(requestedRules, existingActivity)

        assertThat(newRules).isEmpty()
      }

      @Test
      fun `should add a single rule`() {
        val existingActivity = CourseActivityBuilderFactory().builder(courseScheduleRules = listOf()).create()
        val requestedRules = listOf(scheduleRuleRequest)

        val newRules = scheduleRuleService.buildNewRules(requestedRules, existingActivity)

        assertThat(newRules.size).isEqualTo(1)
        with(newRules.first()) {
          assertNewRule()
          assertThat(startTime).isEqualTo("${monthStart}T09:30")
          assertThat(endTime).isEqualTo("${monthStart}T12:30")
          assertThat(monday).isEqualTo(true)
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
      }

      @Test
      fun `should add multiple rules`() {
        val existingActivity = CourseActivityBuilderFactory().builder(courseScheduleRules = listOf()).create()
        val requestedRules = listOf(
          scheduleRuleRequest,
          scheduleRuleRequest.copy(startTime = LocalTime.of(14, 0), endTime = LocalTime.of(15, 30), monday = false),
        )

        val newRules = scheduleRuleService.buildNewRules(requestedRules, existingActivity)

        assertThat(newRules.size).isEqualTo(2)
        with(newRules[0]) {
          assertNewRule()
          assertThat(startTime).isEqualTo("${monthStart}T09:30")
          assertThat(endTime).isEqualTo("${monthStart}T12:30")
          assertThat(monday).isEqualTo(true)
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
        with(newRules[1]) {
          assertNewRule()
          assertThat(startTime).isEqualTo("${monthStart}T14:00")
          assertThat(endTime).isEqualTo("${monthStart}T15:30")
          assertThat(monday).isEqualTo(false)
          assertThat(slotCategory).isEqualTo(SlotCategory.PM)
        }
      }

      @Test
      fun `should do nothing for an unchanged rule`() {
        val existingActivity =
          CourseActivityBuilderFactory().builder(courseScheduleRules = listOf(CourseScheduleRuleBuilder(id = 1)))
            .create()
        val requestedRules = listOf(scheduleRuleRequest)

        val newRules = scheduleRuleService.buildNewRules(requestedRules, existingActivity)

        assertThat(newRules.size).isEqualTo(1)
        with(newRules.first()) {
          assertExistingRule()
          assertThat(startTime).isEqualTo("${monthStart}T09:30")
          assertThat(endTime).isEqualTo("${monthStart}T12:30")
          assertThat(monday).isEqualTo(true)
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
      }

      @Test
      fun `should remove a single rule`() {
        val existingActivity =
          CourseActivityBuilderFactory().builder(courseScheduleRules = listOf(CourseScheduleRuleBuilder(id = 1)))
            .create()
        val requestedRules = listOf<ScheduleRuleRequest>()

        val newRules = scheduleRuleService.buildNewRules(requestedRules, existingActivity)

        assertThat(newRules).isEmpty()
      }

      @Test
      fun `should remove and add a changed rule`() {
        val existingActivity =
          CourseActivityBuilderFactory().builder(courseScheduleRules = listOf(CourseScheduleRuleBuilder(id = 1)))
            .create()
        val requestedRules = listOf(scheduleRuleRequest.copy(monday = false))

        val newRules = scheduleRuleService.buildNewRules(requestedRules, existingActivity)

        assertThat(newRules.size).isEqualTo(1)
        with(newRules.first()) {
          assertNewRule()
          assertThat(startTime).isEqualTo("${monthStart}T09:30")
          assertThat(endTime).isEqualTo("${monthStart}T12:30")
          assertThat(monday).isEqualTo(false)
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
      }

      @Test
      fun `should add an extra rule`() {
        val existingActivity =
          CourseActivityBuilderFactory().builder(courseScheduleRules = listOf(CourseScheduleRuleBuilder(id = 1)))
            .create()
        val requestedRules = listOf(
          scheduleRuleRequest,
          scheduleRuleRequest.copy(startTime = LocalTime.of(14, 0), endTime = LocalTime.of(15, 0)),
        )

        val newRules = scheduleRuleService.buildNewRules(requestedRules, existingActivity)

        assertThat(newRules.size).isEqualTo(2)
        with(newRules[0]) {
          assertExistingRule()
          assertThat(startTime).isEqualTo("${monthStart}T09:30")
          assertThat(endTime).isEqualTo("${monthStart}T12:30")
          assertThat(monday).isEqualTo(true)
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
        with(newRules[1]) {
          assertNewRule()
          assertThat(startTime).isEqualTo("${monthStart}T14:00")
          assertThat(endTime).isEqualTo("${monthStart}T15:00")
          assertThat(monday).isEqualTo(true)
          assertThat(slotCategory).isEqualTo(SlotCategory.PM)
        }
      }

      @Test
      fun `should handle add, remove, unchanged and changed rules at the same time`() {
        val existingActivity = CourseActivityBuilderFactory().builder(
          courseScheduleRules = listOf(
            CourseScheduleRuleBuilder(id = 1),
            CourseScheduleRuleBuilder(id = 2, startTimeHours = 14, endTimeHours = 15, thursday = false, friday = false),
            CourseScheduleRuleBuilder(
              id = 3,
              startTimeHours = 13,
              endTimeHours = 15,
              monday = false,
              tuesday = false,
              wednesday = false,
            ),
          ),
        ).create()
        val requestedRules = listOf(
          scheduleRuleRequest.copy(
            startTime = LocalTime.of(14, 15),
            endTime = LocalTime.of(15, 15),
            thursday = false,
            friday = false,
          ),
          scheduleRuleRequest.copy(
            startTime = LocalTime.of(13, 30),
            endTime = LocalTime.of(15, 30),
            monday = false,
            tuesday = false,
            wednesday = false,
          ),
          scheduleRuleRequest.copy(startTime = LocalTime.of(19, 0), endTime = LocalTime.of(21, 0)),
        )

        val newRules = scheduleRuleService.buildNewRules(requestedRules, existingActivity)

        assertThat(newRules.size).isEqualTo(3)
        // recreates the Monday afternoon rule with new times
        with(newRules[0]) {
          assertNewRule()
          assertThat(startTime).isEqualTo("${monthStart}T14:15")
          assertThat(endTime).isEqualTo("${monthStart}T15:15")
          assertThat(monday).isEqualTo(true)
          assertThat(thursday).isEqualTo(false)
          assertThat(slotCategory).isEqualTo(SlotCategory.PM)
        }
        // retains the unchanged Thursday afternoon rule
        with(newRules[1]) {
          assertExistingRule()
          assertThat(startTime).isEqualTo("${monthStart}T13:30")
          assertThat(endTime).isEqualTo("${monthStart}T15:30")
          assertThat(monday).isEqualTo(false)
          assertThat(thursday).isEqualTo(true)
          assertThat(slotCategory).isEqualTo(SlotCategory.PM)
        }
        // creates the new evening rule
        with(newRules[2]) {
          assertNewRule()
          assertThat(startTime).isEqualTo("${monthStart}T19:00")
          assertThat(endTime).isEqualTo("${monthStart}T21:00")
          assertThat(monday).isEqualTo(true)
          assertThat(slotCategory).isEqualTo(SlotCategory.ED)
        }
      }

      private fun CourseScheduleRule.assertNewRule() = assertThat(this.id).isEqualTo(0)
      private fun CourseScheduleRule.assertExistingRule() = assertThat(this.id).isGreaterThan(0)
    }
  }

  @Nested
  inner class BuildUpdateTelemetry {

    private val courseActivity = CourseActivityBuilderFactory().builder(startDate = "2022-10-31").create()

    @Test
    fun `should publish deletions, creation and update of rules`() {
      val oldRules = listOf(
        CourseScheduleRuleBuilder(id = 1).build(courseActivity = courseActivity),
        CourseScheduleRuleBuilder(id = 2, startTimeHours = 10).build(courseActivity = courseActivity),
      )
      val newRules = listOf(
        CourseScheduleRuleBuilder(id = 1).build(courseActivity = courseActivity),
        CourseScheduleRuleBuilder(id = 3, startTimeHours = 11).build(courseActivity),
        CourseScheduleRuleBuilder(id = 4, saturday = false).build(courseActivity = courseActivity),
      )

      val telemetry = scheduleRuleService.buildUpdateTelemetry(oldRules, newRules)

      assertThat(telemetry["removed-courseScheduleRuleIds"]).isEqualTo("[2]")
      assertThat(telemetry["created-courseScheduleRuleIds"]).isEqualTo("[3, 4]")
    }
  }
}
