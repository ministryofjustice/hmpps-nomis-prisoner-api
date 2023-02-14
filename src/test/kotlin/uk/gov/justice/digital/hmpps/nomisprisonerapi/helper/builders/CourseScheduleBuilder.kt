package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate

class CourseScheduleBuilder(
  val scheduleDate: String = "2022-11-01",
  val startTimeHours: Int = 8,
  val startTimeMinutes: Int = 0,
  val endTimeHours: Int = 11,
  val endTimeMinutes: Int = 0,
  val slotCategory: SlotCategory = SlotCategory.AM,
) {
  fun build(courseActivity: CourseActivity) =
    LocalDate.parse(scheduleDate).let { date ->
      CourseSchedule(
        courseActivity = courseActivity,
        scheduleDate = date,
        startTime = date.atTime(startTimeHours, startTimeMinutes),
        endTime = date.atTime(endTimeHours, endTimeMinutes),
        slotCategory = slotCategory,
      )
    }
}
