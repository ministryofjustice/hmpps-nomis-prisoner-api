package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import java.time.LocalDate

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
      startTime = "09:00",
      endTime = "12:00"
    )

    @Nested
    inner class Validation {
      @Test // TODO SDI-610 move to integration test
      fun `should throw on invalid schedule date`() {
      }

      @Test
      fun `should throw if schedule date before activity start date`() {
        val request = createActivityRequest.copy(schedules = listOf(createSchedulesRequest.copy(date = LocalDate.of(2022, 10, 30))))

        assertThatThrownBy {
          scheduleService.mapSchedules(request, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule for date 2022-10-30 is before the activity starts")
      }

      @ParameterizedTest
      @ValueSource(strings = ["INVALID", "", "24:00", "08:60"])
      fun `should throw if invalid start time`(startTime: String) {
        val request = createActivityRequest.copy(schedules = listOf(createSchedulesRequest.copy(startTime = startTime)))

        assertThatThrownBy {
          scheduleService.mapSchedules(request, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule for date 2022-11-01 has invalid start time $startTime")
      }

      @ParameterizedTest
      @ValueSource(strings = ["INVALID", "", "24:00", "08:60"])
      fun `should throw if invalid end time`(endTime: String) {
        val request = createActivityRequest.copy(schedules = listOf(createSchedulesRequest.copy(endTime = endTime)))

        assertThatThrownBy {
          scheduleService.mapSchedules(request, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule for date 2022-11-01 has invalid end time $endTime")
      }

      @Test
      fun `should throw if starts after end time`() {
        val request = createActivityRequest.copy(schedules = listOf(createSchedulesRequest.copy(startTime = "09:01", endTime = "09:00")))

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
            createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 2), startTime = "10:00", endTime = "13:00")
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
    }
  }
}
