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

@TestDataDslMarker
interface CourseScheduleDsl

@Component
class CourseScheduleBuilderRepository(val courseScheduleRepository: CourseScheduleRepository) {
  fun save(courseSchedule: CourseSchedule) = courseScheduleRepository.save(courseSchedule)
}

@Component
class CourseScheduleBuilderFactory(val repository: CourseScheduleBuilderRepository? = null) {
  fun builder(
    courseScheduleId: Long,
    scheduleDate: String,
    startTime: String,
    endTime: String,
    slotCategory: SlotCategory,
    scheduleStatus: String,
  ) = CourseScheduleBuilder(repository, courseScheduleId, scheduleDate, startTime, endTime, slotCategory, scheduleStatus)
}

class CourseScheduleBuilder(
  val repository: CourseScheduleBuilderRepository? = null,
  val courseScheduleId: Long,
  val scheduleDate: String,
  val startTime: String,
  val endTime: String,
  val slotCategory: SlotCategory,
  val scheduleStatus: String,
) : CourseScheduleDsl {
  fun build(courseActivity: CourseActivity) =
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

  fun save(courseSchedule: CourseSchedule) = repository?.save(courseSchedule) ?: courseSchedule
}
