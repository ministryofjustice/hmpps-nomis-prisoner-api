package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import java.time.LocalDate
import java.time.LocalTime

@DslMarker
annotation class CourseScheduleDslMarker

@NomisDataDslMarker
interface CourseScheduleDsl

interface CourseScheduleDslApi {
  @CourseScheduleDslMarker
  fun courseSchedule(
    courseScheduleId: Long = 0,
    scheduleDate: String = "2022-11-01",
    startTime: String = "08:00",
    endTime: String = "11:00",
    slotCategory: SlotCategory = SlotCategory.AM,
    scheduleStatus: String = "SCH",
  ): CourseSchedule
}

@Component
class CourseScheduleBuilderRepository(val courseScheduleRepository: CourseScheduleRepository) {
  fun save(courseSchedule: CourseSchedule) = courseScheduleRepository.save(courseSchedule)
}

@Component
class CourseScheduleBuilderFactory(val repository: CourseScheduleBuilderRepository? = null) {
  fun builder() = CourseScheduleBuilder(repository)
}

class CourseScheduleBuilder(val repository: CourseScheduleBuilderRepository? = null) : CourseScheduleDsl {
  fun build(
    courseActivity: CourseActivity,
    courseScheduleId: Long,
    scheduleDate: String,
    startTime: String,
    endTime: String,
    slotCategory: SlotCategory,
    scheduleStatus: String,
  ) =
    LocalDate.parse(scheduleDate).let { date ->
      CourseSchedule(
        courseScheduleId = courseScheduleId,
        courseActivity = courseActivity,
        scheduleDate = date,
        startTime = date.atTime(LocalTime.parse(startTime)),
        endTime = date.atTime(LocalTime.parse(endTime)),
        slotCategory = slotCategory,
        scheduleStatus = scheduleStatus,
      ).let {
        save(it)
      }
    }

  private fun save(courseSchedule: CourseSchedule) = repository?.save(courseSchedule) ?: courseSchedule
}
