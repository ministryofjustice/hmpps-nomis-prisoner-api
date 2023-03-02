package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseScheduleBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.AM
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.ED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.PM
import java.time.LocalDate
import java.time.LocalTime

class ScheduleServiceTest {

  private val scheduleService = ScheduleService()

  @Nested
  inner class CreateSchedules {

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
        val requestedSchedules = listOf(createSchedulesRequest.copy(date = LocalDate.of(2022, 10, 30)))

        assertThatThrownBy {
          scheduleService.mapSchedules(requestedSchedules, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule for date 2022-10-30 is before the activity starts")
      }

      @Test
      fun `should throw if starts after end time`() {
        val requestedSchedules = listOf(
          createSchedulesRequest.copy(startTime = LocalTime.of(9, 1), endTime = LocalTime.of(9, 0)),
        )

        assertThatThrownBy {
          scheduleService.mapSchedules(requestedSchedules, courseActivity)
        }
          .isInstanceOf(BadDataException::class.java)
          .hasMessageContaining("Schedule for date 2022-11-01 has times out of order - 09:01 to 09:00")
      }
    }

    @Nested
    inner class Create {
      @Test
      fun `should do nothing if no schedules`() {
        val requestedSchedules = listOf<SchedulesRequest>()

        val newSchedules = scheduleService.mapSchedules(requestedSchedules, courseActivity)

        assertThat(newSchedules).isEmpty()
      }

      @Test
      fun `should save a single schedule`() {
        val requestedSchedules = listOf(createSchedulesRequest)

        val newSchedules = scheduleService.mapSchedules(requestedSchedules, courseActivity)

        assertThat(newSchedules.size).isEqualTo(1)
        with(newSchedules[0]) {
          assertThat(scheduleDate).isEqualTo("2022-11-01")
          assertThat(startTime).isEqualTo("2022-11-01T09:00")
          assertThat(endTime).isEqualTo("2022-11-01T12:00")
        }
      }

      @Test
      fun `should save multiple schedules`() {
        val requestedSchedules = listOf(
          createSchedulesRequest,
          createSchedulesRequest.copy(
            date = LocalDate.of(2022, 11, 2),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(13, 0),
          ),
        )

        val newSchedules = scheduleService.mapSchedules(requestedSchedules, courseActivity)

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
        val requestedSchedules = listOf(
          createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 2), startTime = LocalTime.of(0, 0), endTime = LocalTime.of(11, 59)),
          createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 2), startTime = LocalTime.of(12, 0), endTime = LocalTime.of(16, 59)),
          createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 2), startTime = LocalTime.of(17, 0), endTime = LocalTime.of(23, 59)),
          createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 3), startTime = LocalTime.of(11, 59), endTime = LocalTime.of(11, 59)),
          createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 3), startTime = LocalTime.of(16, 59), endTime = LocalTime.of(16, 59)),
          createSchedulesRequest.copy(date = LocalDate.of(2022, 11, 3), startTime = LocalTime.of(23, 59), endTime = LocalTime.of(23, 59)),
        )

        val newSchedules = scheduleService.mapSchedules(requestedSchedules, courseActivity)

        assertThat(newSchedules).extracting("slotCategory").containsExactly(AM, PM, ED, AM, PM, ED)
      }
    }
  }

  @Nested
  inner class UpdateSchedules {

    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)
    private val tomorrow = today.plusDays(1)
    private val twoDaysTime = today.plusDays(2)
    private val threeDaysTime = today.plusDays(3)
    private val fourDaysTime = today.plusDays(4)

    private val courseSchedules = listOf(
      CourseScheduleBuilder(scheduleDate = today.toString()),
      CourseScheduleBuilder(scheduleDate = tomorrow.toString()),
    )
    private val courseActivity =
      CourseActivityBuilderFactory().builder(startDate = yesterday.toString(), courseSchedules = courseSchedules)
        .create()
    private val requestTemplate = SchedulesRequest(
      date = today,
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(11, 0),
    )

    @Test
    fun `should throw if adding a new schedule before tomorrow`() {
      val request = listOf(requestTemplate.copy(date = yesterday), requestTemplate.copy(date = tomorrow))

      assertThatThrownBy {
        scheduleService.updateSchedules(request, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Cannot update schedules starting before tomorrow")
    }

    @Test
    fun `should throw if changing a schedule before tomorrow`() {
      val request = listOf(requestTemplate.copy(startTime = LocalTime.of(9, 0)), requestTemplate.copy(date = tomorrow))

      assertThatThrownBy {
        scheduleService.updateSchedules(request, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Cannot update schedules starting before tomorrow")
    }

    @Test
    fun `should throw if removing a schedule before tomorrow`() {
      val request = listOf(requestTemplate.copy(date = tomorrow))

      assertThatThrownBy {
        scheduleService.updateSchedules(request, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Cannot remove or add schedules starting before tomorrow")
    }

    @Test
    fun `should throw if adding a schedule before tomorrow`() {
      val request = listOf(requestTemplate.copy(date = yesterday), requestTemplate, requestTemplate.copy(date = tomorrow))

      assertThatThrownBy {
        scheduleService.updateSchedules(request, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Cannot remove or add schedules starting before tomorrow")
    }

    @Test
    fun `should return the old schedule retrieved from the database (and not a copy)`() {
      val request = listOf(requestTemplate, requestTemplate.copy(date = tomorrow))

      val updatedSchedules = scheduleService.updateSchedules(request, courseActivity)

      assertThat(updatedSchedules.first()).isSameAs(courseActivity.courseSchedules.first())
    }

    @Test
    fun `should return updated future schedule`() {
      val request = listOf(requestTemplate, requestTemplate.copy(date = tomorrow, startTime = LocalTime.of(9, 0)))

      val updatedSchedules = scheduleService.updateSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(2)
      with(updatedSchedules[1]) {
        assertThat(scheduleDate).isEqualTo(tomorrow.toString())
        assertThat(startTime).isEqualTo("${tomorrow}T09:00:00")
        assertThat(endTime).isEqualTo("${tomorrow}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
      }
    }

    @Test
    fun `should return future schedule moved to another day`() {
      val request = listOf(requestTemplate, requestTemplate.copy(date = twoDaysTime))

      val updatedSchedules = scheduleService.updateSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(2)
      with(updatedSchedules[1]) {
        assertThat(scheduleDate).isEqualTo(twoDaysTime.toString())
        assertThat(startTime).isEqualTo("${twoDaysTime}T08:00:00")
        assertThat(endTime).isEqualTo("${twoDaysTime}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
      }
    }

    @Test
    fun `should return the existing future schedule from database if not changed (not a copy)`() {
      val request = listOf(requestTemplate, requestTemplate.copy(date = tomorrow))

      val updatedSchedules = scheduleService.updateSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(2)
      assertThat(updatedSchedules[1]).isSameAs(courseActivity.courseSchedules[1])
    }

    @Test
    fun `should return brand new schedule`() {
      val request = listOf(requestTemplate, requestTemplate.copy(date = tomorrow), requestTemplate.copy(date = twoDaysTime))

      val updatedSchedules = scheduleService.updateSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(3)
      with(updatedSchedules[1]) {
        assertThat(scheduleDate).isEqualTo(tomorrow.toString())
        assertThat(startTime).isEqualTo("${tomorrow}T08:00:00")
        assertThat(endTime).isEqualTo("${tomorrow}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
      }
      with(updatedSchedules[2]) {
        assertThat(scheduleDate).isEqualTo(twoDaysTime.toString())
        assertThat(startTime).isEqualTo("${twoDaysTime}T08:00:00")
        assertThat(endTime).isEqualTo("${twoDaysTime}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
      }
    }

    @Test
    fun `should throw if adding an invalid schedule`() {
      val request = listOf(requestTemplate, requestTemplate.copy(date = tomorrow, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(7, 59)))

      assertThatThrownBy {
        scheduleService.updateSchedules(request, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Schedule for date $tomorrow has times out of order - 08:00 to 07:59")
    }

    @Test
    fun `should handle multiple past and future schedules with deletes, additions and updates`() {
      val courseSchedules = listOf(
        CourseScheduleBuilder(scheduleDate = yesterday.toString()),
        CourseScheduleBuilder(scheduleDate = today.toString()),
        CourseScheduleBuilder(scheduleDate = tomorrow.toString()),
        CourseScheduleBuilder(scheduleDate = twoDaysTime.toString()),
        CourseScheduleBuilder(scheduleDate = threeDaysTime.toString()),
      )
      val courseActivity =
        CourseActivityBuilderFactory().builder(startDate = yesterday.toString(), courseSchedules = courseSchedules).create()
      val request = listOf(
        requestTemplate.copy(date = yesterday),
        requestTemplate.copy(date = today),
        requestTemplate.copy(date = twoDaysTime, startTime = LocalTime.of(13, 0), endTime = LocalTime.of(15, 0)),
        requestTemplate.copy(date = threeDaysTime),
        requestTemplate.copy(date = fourDaysTime),
      )

      val updatedSchedules = scheduleService.updateSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(5)
      // past schedules
      with(updatedSchedules[0]) {
        assertThat(scheduleDate).isEqualTo(yesterday.toString())
        assertThat(this).isSameAs(courseActivity.courseSchedules[0])
      }
      with(updatedSchedules[1]) {
        assertThat(scheduleDate).isEqualTo(today.toString())
        assertThat(this).isSameAs(courseActivity.courseSchedules[1])
      }
      // deleted
      assertThat(updatedSchedules.find { it.scheduleDate == tomorrow }).isNull()
      // updated
      with(updatedSchedules[2]) {
        assertThat(scheduleDate).isEqualTo(twoDaysTime.toString())
        assertThat(startTime).isEqualTo("${twoDaysTime}T13:00:00")
        assertThat(endTime).isEqualTo("${twoDaysTime}T15:00:00")
        assertThat(slotCategory).isEqualTo(PM)
        // this is a delete and add so existing not updated
        assertThat(this).isNotSameAs(courseActivity.courseSchedules[3])
      }
      // unchanged
      with(updatedSchedules[3]) {
        assertThat(scheduleDate).isEqualTo(threeDaysTime.toString())
        assertThat(this).isSameAs(courseActivity.courseSchedules[4])
      }
      // added
      with(updatedSchedules[4]) {
        assertThat(scheduleDate).isEqualTo(fourDaysTime.toString())
        assertThat(startTime).isEqualTo("${fourDaysTime}T08:00:00")
        assertThat(endTime).isEqualTo("${fourDaysTime}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
        courseActivity.courseSchedules.forEach {
          assertThat(this).isNotSameAs(it)
        }
      }
    }
  }
}
