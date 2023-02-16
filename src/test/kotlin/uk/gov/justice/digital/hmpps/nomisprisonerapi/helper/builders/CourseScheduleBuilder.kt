package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate
import java.time.LocalTime

class CourseScheduleBuilder(
  val scheduleDate: String = "2022-11-01",
  val startTime: String = "08:00",
  val endTime: String = "11:00",
  val slotCategory: SlotCategory = SlotCategory.AM,
) {
  fun build(courseActivity: CourseActivity) =
    LocalDate.parse(scheduleDate).let { date ->
      CourseSchedule(
        courseActivity = courseActivity,
        scheduleDate = date,
        startTime = date.atTime(LocalTime.parse(startTime)),
        endTime = date.atTime(LocalTime.parse(endTime)),
        slotCategory = slotCategory,
      )
    }
}
