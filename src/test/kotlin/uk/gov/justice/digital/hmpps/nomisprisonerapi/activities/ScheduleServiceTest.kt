package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CourseScheduleRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.AM
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.ED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory.PM
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import java.time.LocalDate
import java.time.LocalTime

class ScheduleServiceTest {

  private val nomisDataBuilder = NomisDataBuilder()
  private lateinit var courseActivity: CourseActivity
  private val scheduleRepository: CourseScheduleRepository = mock()
  private val activityRepository: CourseActivityRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val scheduleService = ScheduleService(scheduleRepository, activityRepository, telemetryClient)

  @Nested
  inner class CreateSchedules {

    private val createSchedulesRequest = CourseScheduleRequest(
      date = LocalDate.of(2022, 11, 1),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(12, 0),
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity()
        }
      }
    }

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
        val requestedSchedules = listOf<CourseScheduleRequest>()

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

    private val requestTemplate = CourseScheduleRequest(
      date = today,
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(11, 0),
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            courseScheduleRule()
            payRate()
            courseSchedule(courseScheduleId = 1, scheduleDate = yesterday.toString())
            courseSchedule(courseScheduleId = 2, scheduleDate = today.toString())
          }
        }
      }
    }

    @Test
    fun `should ignore changing a schedule before today`() {
      val request = listOf(
        requestTemplate.copy(id = 1, date = yesterday, startTime = LocalTime.of(11, 0)),
        requestTemplate.copy(id = 2, date = today),
      )

      val newSchedules = scheduleService.buildNewSchedules(request, courseActivity)

      with(newSchedules[0]) {
        assertThat(courseScheduleId).isEqualTo(1)
        assertThat(startTime).isEqualTo("${yesterday}T08:00:00") // change of time ignored
      }
    }

    @Test
    fun `should ignore removing a schedule before today`() {
      val request = listOf(requestTemplate.copy(id = 2, date = today))

      val newSchedules = scheduleService.buildNewSchedules(request, courseActivity)

      assertThat(newSchedules.size).isEqualTo(2)
      with(newSchedules[0]) {
        assertThat(courseScheduleId).isEqualTo(1)
        assertThat(startTime).isEqualTo("${yesterday}T08:00:00")
      }
      with(newSchedules[1]) {
        assertThat(courseScheduleId).isEqualTo(2)
        assertThat(startTime).isEqualTo("${today}T08:00:00")
      }
    }

    @Test
    fun `should allow adding a schedule before today`() {
      val request = listOf(
        requestTemplate.copy(date = yesterday, startTime = LocalTime.parse("13:00"), endTime = LocalTime.parse("15:00")),
        requestTemplate.copy(id = 1, date = yesterday),
        requestTemplate.copy(id = 2, date = today),
      )

      val newSchedules = scheduleService.buildNewSchedules(request, courseActivity)

      assertThat(newSchedules.size).isEqualTo(3)
      with(newSchedules[0]) {
        assertThat(courseScheduleId).isEqualTo(1)
        assertThat(startTime).isEqualTo("${yesterday}T08:00:00")
      }
      with(newSchedules[1]) {
        assertThat(courseScheduleId).isEqualTo(2)
        assertThat(startTime).isEqualTo("${today}T08:00:00")
      }
      with(newSchedules[2]) {
        assertThat(courseScheduleId).isEqualTo(0)
        assertThat(startTime).isEqualTo("${yesterday}T13:00:00")
      }
    }

    @Test
    fun `should throw if schedule to update not found`() {
      val request = listOf(requestTemplate.copy(id = 99999L))

      assertThatThrownBy {
        scheduleService.buildNewSchedules(request, courseActivity)
      }
        .isInstanceOf(NotFoundException::class.java)
        .hasMessageContaining("Course schedule 99999 does not exist")
    }

    @Test
    fun `should return schedules retrieved from the database (and not a copy)`() {
      val request = listOf(
        requestTemplate.copy(id = 1, date = yesterday),
        requestTemplate.copy(id = 2, date = today),
      )

      val updatedSchedules = scheduleService.buildNewSchedules(request, courseActivity)

      assertThat(updatedSchedules[0]).isSameAs(courseActivity.courseSchedules[0])
      assertThat(updatedSchedules[1]).isSameAs(courseActivity.courseSchedules[1])
    }

    @Test
    fun `should return updated future schedule`() {
      val request = listOf(
        requestTemplate.copy(id = 1, date = yesterday),
        requestTemplate.copy(id = 2, date = today, startTime = LocalTime.of(9, 0)),
      )

      val updatedSchedules = scheduleService.buildNewSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(2)
      with(updatedSchedules[1]) {
        assertThat(courseScheduleId).isEqualTo(2)
        assertThat(scheduleDate).isEqualTo(today.toString())
        assertThat(startTime).isEqualTo("${today}T09:00:00")
        assertThat(endTime).isEqualTo("${today}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
      }
    }

    @Test
    fun `should return future schedule moved to another day`() {
      val request = listOf(
        requestTemplate.copy(id = 1, date = yesterday),
        requestTemplate.copy(id = 2, date = twoDaysTime),
      )

      val updatedSchedules = scheduleService.buildNewSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(2)
      with(updatedSchedules[1]) {
        assertThat(courseScheduleId).isEqualTo(2)
        assertThat(scheduleDate).isEqualTo(twoDaysTime.toString())
        assertThat(startTime).isEqualTo("${twoDaysTime}T08:00:00")
        assertThat(endTime).isEqualTo("${twoDaysTime}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
      }
    }

    @Test
    fun `should return brand new schedule`() {
      val request = listOf(
        requestTemplate.copy(id = 1, date = yesterday),
        requestTemplate.copy(id = 2, date = today),
        requestTemplate.copy(date = twoDaysTime),
      )

      val updatedSchedules = scheduleService.buildNewSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(3)
      with(updatedSchedules[1]) {
        assertThat(courseScheduleId).isEqualTo(2)
        assertThat(scheduleDate).isEqualTo(today.toString())
        assertThat(startTime).isEqualTo("${today}T08:00:00")
        assertThat(endTime).isEqualTo("${today}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
      }
      with(updatedSchedules[2]) {
        assertThat(courseScheduleId).isEqualTo(0)
        assertThat(scheduleDate).isEqualTo(twoDaysTime.toString())
        assertThat(startTime).isEqualTo("${twoDaysTime}T08:00:00")
        assertThat(endTime).isEqualTo("${twoDaysTime}T11:00:00")
        assertThat(slotCategory).isEqualTo(AM)
      }
    }

    @Test
    fun `should throw if updating to an invalid schedule`() {
      val request = listOf(
        requestTemplate.copy(id = 1, date = yesterday),
        requestTemplate.copy(id = 2, date = today, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(7, 59)),
      )

      assertThatThrownBy {
        scheduleService.buildNewSchedules(request, courseActivity)
      }
        .isInstanceOf(BadDataException::class.java)
        .hasMessageContaining("Schedule for date $today has times out of order - 08:00 to 07:59")
    }

    @Test
    fun `should handle multiple past and future schedules with deletes, additions and updates`() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            courseScheduleRule()
            payRate()
            courseSchedule(courseScheduleId = 1, scheduleDate = yesterday.toString())
            courseSchedule(courseScheduleId = 2, scheduleDate = today.toString())
            courseSchedule(courseScheduleId = 3, scheduleDate = tomorrow.toString())
            courseSchedule(courseScheduleId = 4, scheduleDate = twoDaysTime.toString())
            courseSchedule(courseScheduleId = 5, scheduleDate = threeDaysTime.toString())
          }
        }
      }
      val request = listOf(
        requestTemplate.copy(id = 1, date = yesterday),
        requestTemplate.copy(id = 2, date = today),
        requestTemplate.copy(id = 4, date = twoDaysTime, startTime = LocalTime.of(13, 0), endTime = LocalTime.of(15, 0)),
        requestTemplate.copy(id = 5, date = threeDaysTime),
        requestTemplate.copy(date = fourDaysTime),
      )

      val updatedSchedules = scheduleService.buildNewSchedules(request, courseActivity)

      assertThat(updatedSchedules.size).isEqualTo(5)
      // past schedules
      with(updatedSchedules[0]) {
        assertThat(courseScheduleId).isEqualTo(1)
        assertThat(scheduleDate).isEqualTo(yesterday.toString())
        assertThat(this).isSameAs(courseActivity.courseSchedules[0])
      }
      with(updatedSchedules[1]) {
        assertThat(courseScheduleId).isEqualTo(2)
        assertThat(scheduleDate).isEqualTo(today.toString())
        assertThat(this).isSameAs(courseActivity.courseSchedules[1])
      }
      // deleted
      assertThat(updatedSchedules.find { it.scheduleDate == tomorrow }).isNull()
      // updated
      with(updatedSchedules[2]) {
        assertThat(courseScheduleId).isEqualTo(4)
        assertThat(scheduleDate).isEqualTo(twoDaysTime.toString())
        assertThat(startTime).isEqualTo("${twoDaysTime}T13:00:00")
        assertThat(endTime).isEqualTo("${twoDaysTime}T15:00:00")
        assertThat(slotCategory).isEqualTo(PM)
      }
      // unchanged
      with(updatedSchedules[3]) {
        assertThat(courseScheduleId).isEqualTo(5)
        assertThat(scheduleDate).isEqualTo(threeDaysTime.toString())
        assertThat(this).isSameAs(courseActivity.courseSchedules[4])
      }
      // added
      with(updatedSchedules[4]) {
        assertThat(courseScheduleId).isEqualTo(0)
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

  @Nested
  inner class BuildUpdateTelemetry {
    private val today = LocalDate.now()
    private val tomorrow = today.plusDays(1)
    private lateinit var oldSchedules: List<CourseSchedule>

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        programService {
          courseActivity(startDate = today.toString()) {
            oldSchedules = listOf(
              courseSchedule(courseScheduleId = 1, scheduleDate = today.toString()),
              courseSchedule(courseScheduleId = 2, scheduleDate = tomorrow.toString()),
            )
          }
        }
      }
    }

    @Test
    fun `should publish deletions and creation of schedules`() {
      val newSchedules = mutableListOf<CourseSchedule>()
      nomisDataBuilder.build {
        programService {
          courseActivity {
            newSchedules.addAll(
              listOf(
                courseSchedule(courseScheduleId = 1, scheduleDate = today.toString()),
                courseSchedule(courseScheduleId = 3, scheduleDate = tomorrow.toString(), startTime = "09:00"),
              ),
            )
          }
        }
      }

      val telemetry = scheduleService.buildUpdateTelemetry(oldSchedules, newSchedules)

      assertThat(telemetry["removed-courseScheduleIds"]).isEqualTo("[2]")
      assertThat(telemetry["created-courseScheduleIds"]).isEqualTo("[3]")
    }

    @Test
    fun `should publish update of schedules`() {
      val newSchedules = mutableListOf<CourseSchedule>()
      nomisDataBuilder.build {
        programService {
          courseActivity {
            newSchedules.addAll(
              listOf(
                courseSchedule(courseScheduleId = 1, scheduleDate = today.toString()),
                courseSchedule(courseScheduleId = 2, scheduleDate = tomorrow.toString(), startTime = "09:00"),
              ),
            )
          }
        }
      }

      val telemetry = scheduleService.buildUpdateTelemetry(oldSchedules, newSchedules)

      assertThat(telemetry["updated-courseScheduleIds"]).isEqualTo("[2]")
    }

    @Test
    fun `should not publish telemetry if nothing changed`() {
      val newSchedules = mutableListOf<CourseSchedule>()
      nomisDataBuilder.build {
        programService {
          courseActivity {
            newSchedules.addAll(
              listOf(
                courseSchedule(courseScheduleId = 1, scheduleDate = today.toString()),
                courseSchedule(courseScheduleId = 2, scheduleDate = tomorrow.toString()),
              ),
            )
          }
        }
      }

      val telemetry = scheduleService.buildUpdateTelemetry(oldSchedules, newSchedules)

      assertThat(telemetry["removed-courseScheduleIds"]).isNull()
      assertThat(telemetry["created-courseScheduleIds"]).isNull()
      assertThat(telemetry["updated-courseScheduleIds"]).isNull()
    }
  }
}
