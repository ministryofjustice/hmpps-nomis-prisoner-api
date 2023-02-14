package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDateTime

private const val HOURS_AND_MINUTES = "([01]?[0-9]|2[0-3]):([0-5][0-9])"

class ScheduleService {

  fun mapSchedules(request: CreateActivityRequest, courseActivity: CourseActivity): List<CourseSchedule> =
    request.schedules.map {

      validateRequest(it, courseActivity)

      val (startHour, startMinute) = getHoursAndMinutes(it.startTime)
      val (endHour, endMinute) = getHoursAndMinutes(it.endTime)

      CourseSchedule(
        courseActivity = courseActivity,
        scheduleDate = it.date,
        startTime = it.date.atTime(startHour, startMinute),
        endTime = it.date.atTime(endHour, endMinute),
        slotCategory = it.date.atTime(startHour, startMinute).toSlot()
      )
    }.toList()

  private fun validateRequest(request: SchedulesRequest, courseActivity: CourseActivity) {
    if (request.date < courseActivity.scheduleStartDate) {
      throw BadDataException("Schedule for date ${request.date} is before the activity starts on ${courseActivity.scheduleStartDate}")
    }

    if (!request.startTime.validTime()) {
      throw BadDataException("Schedule for date ${request.date} has invalid start time ${request.startTime}")
    }

    if (!request.endTime.validTime()) {
      throw BadDataException("Schedule for date ${request.date} has invalid end time ${request.endTime}")
    }

    if (request.endTime < request.startTime) {
      throw BadDataException("Schedule for date ${request.date} has times out of order - ${request.startTime} to ${request.endTime}")
    }
  }

  private fun getHoursAndMinutes(timeString: String): Pair<Int, Int> {
    val (hour, minute) = Regex(HOURS_AND_MINUTES).find(timeString)!!.destructured
    return hour.toInt() to minute.toInt()
  }

  private fun String.validTime() = Regex(HOURS_AND_MINUTES).matches(this)

  private fun LocalDateTime.toSlot() =
    when {
      hour < 12 -> SlotCategory.AM
      hour < 17 -> SlotCategory.PM
      else -> SlotCategory.ED
    }
}
