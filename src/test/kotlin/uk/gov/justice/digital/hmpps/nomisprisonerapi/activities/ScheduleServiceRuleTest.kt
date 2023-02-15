package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleServiceRuleTest {

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
      payPerSession = "H",
      internalLocationId = null,
    )
    private val courseActivity = CourseActivityBuilderFactory().builder(startDate = "2022-10-31").create()
    private val createScheduleRuleRequest = ScheduleRuleRequest(
      daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
      startTime = LocalTime.parse("09:00"),
      endTime = LocalTime.parse("12:00")
    )

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

      @Test
      fun `should throw if no week days`() {
        val request =
          createActivityRequest.copy(scheduleRules = listOf(createScheduleRuleRequest.copy(daysOfWeek = emptyList())))

        assertThatThrownBy {
          scheduleRuleService.mapRules(request, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule rule daysOfWeek is empty list")
      }

      @Test
      fun `should throw if too many week days`() {
        val tooLarge = Array<DayOfWeek>(8) { DayOfWeek.TUESDAY }.asList()
        val request =
          createActivityRequest.copy(scheduleRules = listOf(createScheduleRuleRequest.copy(daysOfWeek = tooLarge)))

        assertThatThrownBy {
          scheduleRuleService.mapRules(request, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule rule daysOfWeek has too many entries")
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

        val newSchedules = scheduleRuleService.mapRules(request, courseActivity)

        assertThat(newSchedules.size).isEqualTo(1)
        with(newSchedules[0]) {
          assertThat(monday).isTrue
          assertThat(tuesday).isFalse
          assertThat(wednesday).isTrue
          assertThat(thursday).isFalse
          assertThat(friday).isFalse
          assertThat(saturday).isTrue
          assertThat(sunday).isTrue
          assertThat(startTime).isEqualTo("2022-10-01T09:00")
          assertThat(endTime).isEqualTo("2022-10-01T12:00")
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
      }

      @Test
      fun `should save multiple schedules`() {
        val request = createActivityRequest.copy(
          scheduleRules = listOf(
            createScheduleRuleRequest,
            createScheduleRuleRequest.copy(startTime = LocalTime.parse("08:00")),
          )
        )

        val newSchedules = scheduleRuleService.mapRules(request, courseActivity)

        assertThat(newSchedules.size).isEqualTo(2)
        with(newSchedules[0]) {
          assertThat(startTime).isEqualTo("2022-10-01T09:00")
        }
        with(newSchedules[1]) {
          assertThat(startTime).isEqualTo("2022-10-01T08:00")
        }
      }
    }
  }
}
