package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate
import java.time.LocalTime

@DslMarker
annotation class CourseScheduleDslMarker

@NomisDataDslMarker
interface CourseScheduleDsl

@Component
class CourseScheduleBuilderFactory {
  fun builder() = CourseScheduleBuilder()
}

class CourseScheduleBuilder : CourseScheduleDsl {
  fun build(
    courseActivity: CourseActivity,
    courseScheduleId: Long,
    scheduleDate: String,
    startTime: String,
    endTime: String,
    slotCategory: SlotCategory,
    scheduleStatus: String,
  ) = LocalDate.parse(scheduleDate).let { date ->
    CourseSchedule(
      courseScheduleId = courseScheduleId,
      courseActivity = courseActivity,
      scheduleDate = date,
      startTime = date.atTime(LocalTime.parse(startTime)),
      endTime = date.atTime(LocalTime.parse(endTime)),
      slotCategory = slotCategory,
      scheduleStatus = scheduleStatus,
    )
  }
}
