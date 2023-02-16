package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.AM
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.ED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.PM
import java.time.LocalDate
import java.time.LocalTime

class ScheduleServiceTest {

  private val scheduleService = ScheduleService()
  @Nested
  inner class CreateSchedules {

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
    private val createSchedulesRequest = SchedulesRequest(
      date = LocalDate.of(2022, 11, 1),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(12, 0),
    )

    @Nested
    inner class Validation {
      @Test
      fun `should throw if schedule date before activity start date`() {
        val request = createActivityRequest.copy(schedules = listOf(createSchedulesRequest.copy(date = LocalDate.of(2022, 10, 30))))

        assertThatThrownBy {
          scheduleService.mapSchedules(request, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule for date 2022-10-30 is before the activity starts")
      }

      @Test
      fun `should throw if starts after end time`() {
        val request = createActivityRequest.copy(
          schedules = listOf(
            createSchedulesRequest.copy(startTime = LocalTime.of(9, 1), endTime = LocalTime.of(9, 0))
          )
        )

        assertThatThrownBy {
          scheduleService.mapSchedules(request, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule for date 2022-11-01 has times out of order - 09:01 to 09:00")
      }
    }

    @Nested
    inner class Create {
      @Test
      fun `should do nothing if no schedules`() {
        val request = createActivityRequest.copy(schedules = listOf())

        val newSchedules = scheduleService.mapSchedules(request, courseActivity)

        assertThat(newSchedules).isEmpty()
      }

      @Test
      fun `should save a single schedule`() {
        val request = createActivityRequest.copy(schedules = listOf(createSchedulesRequest))

        val newSchedules = scheduleService.mapSchedules(request, courseActivity)

        assertThat(newSchedules.size).isEqualTo(1)
        with(newSchedules[0]) {
          assertThat(scheduleDate).isEqualTo("2022-11-01")
          assertThat(startTime).isEqualTo("2022-11-01T09:00")
          assertThat(endTime).isEqualTo("2022-11-01T12:00")
        }
      }

      @Test
      fun `should save multiple schedules`() {
        val request = createActivityRequest.copy(
          schedules = listOf(
            createSchedulesRequest,
            createSchedulesRequest.copy(
              date = LocalDate.of(2022, 11, 2),
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(13, 0)
            )
          )
        )

        val newSchedules = scheduleService.mapSchedules(request, courseActivity)

        assertThat(newSchedules.size).isEqualTo(2)
        with(newSchedules[0]) {
          assertThat(scheduleDate).isEqualTo("2022-11-01")
          assertThat(startTime).isEqualTo("2022-11-01T09:00")
          assertThat(endTime).isEqualTo("2022-11-01T12:00")
        }
        with(newSchedules[1]) {
          assertThat(scheduleDate).isEqualTo("2022-11-02")
          assertThat(startTime).isEqualTo("2022-11-02T10:00")
          assertThat(endTime).isEqualTo("2022-11-02T13:00")
        }
      }

      @Test
      fun `should default slots`() {
        val request = createActivityRequest.copy(
          schedules = listOf(
            createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 2), startTime = LocalTime.of(0, 0), endTime = LocalTime.of(11, 59)),
            createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 2), startTime = LocalTime.of(12, 0), endTime = LocalTime.of(16, 59)),
            createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 2), startTime = LocalTime.of(17, 0), endTime = LocalTime.of(23, 59)),
            createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 3), startTime = LocalTime.of(11, 59), endTime = LocalTime.of(11, 59)),
            createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 3), startTime = LocalTime.of(16, 59), endTime = LocalTime.of(16, 59)),
            createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 3), startTime = LocalTime.of(23, 59), endTime = LocalTime.of(23, 59)),
          )
        )

        val newSchedules = scheduleService.mapSchedules(request, courseActivity)

        assertThat(newSchedules).extracting("slotCategory").containsExactly(AM, PM, ED, AM, PM, ED)
      }
    }
  }
}
