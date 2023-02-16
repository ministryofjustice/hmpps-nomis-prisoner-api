package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory

@Service
class ScheduleService {

  fun mapSchedules(request: CreateActivityRequest, courseActivity: CourseActivity): List<CourseSchedule> =
    request.schedules.map {

      validateRequest(it, courseActivity)

      CourseSchedule(
        courseActivity = courseActivity,
        scheduleDate = it.date,
        startTime = it.date.atTime(it.startTime),
        endTime = it.date.atTime(it.endTime),
        slotCategory = SlotCategory.of(it.startTime)
      )
    }.toList()

  private fun validateRequest(request: SchedulesRequest, courseActivity: CourseActivity) {
    if (request.date < courseActivity.scheduleStartDate) {
      throw BadDataException("Schedule for date ${request.date} is before the activity starts on ${courseActivity.scheduleStartDate}")
    }

    if (request.endTime < request.startTime) {
      throw BadDataException("Schedule for date ${request.date} has times out of order - ${request.startTime} to ${request.endTime}")
    }
  }
}
