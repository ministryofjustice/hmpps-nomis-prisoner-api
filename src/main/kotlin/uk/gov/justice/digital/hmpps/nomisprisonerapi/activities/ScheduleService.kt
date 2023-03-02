package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import java.time.LocalDate

@Service
class ScheduleService {

  fun mapSchedules(requests: List<SchedulesRequest>, courseActivity: CourseActivity): List<CourseSchedule> =
    requests.map {
      validateRequest(it, courseActivity)

      CourseSchedule(
        courseActivity = courseActivity,
        scheduleDate = it.date,
        startTime = it.date.atTime(it.startTime),
        endTime = it.date.atTime(it.endTime),
        slotCategory = SlotCategory.of(it.startTime),
      )
    }.toList()

  fun updateSchedules(scheduleRequests: List<SchedulesRequest>, courseActivity: CourseActivity): List<CourseSchedule> =
    mapSchedules(scheduleRequests, courseActivity)
      .let { requestedSchedules ->
        findPastSchedules(requestedSchedules, courseActivity) + findOrAddFutureSchedules(requestedSchedules, courseActivity)
      }

  private fun findPastSchedules(requestedSchedules: List<CourseSchedule>, courseActivity: CourseActivity): List<CourseSchedule> {
    val savedPastSchedules = courseActivity.courseSchedules.filterNot { it.isFutureSchedule() }
    val requestedPastSchedules = requestedSchedules.filterNot { it.isFutureSchedule() }

    if (savedPastSchedules.size != requestedPastSchedules.size) {
      throw BadDataException("Cannot remove or add schedules starting before tomorrow")
    }

    return savedPastSchedules.map { savedSchedule ->
      requestedPastSchedules
        .find { requestedSchedule -> requestedSchedule matches savedSchedule }
        ?.let { savedSchedule }
        ?: let { throw BadDataException("Cannot update schedules starting before tomorrow") }
    }
      .toList()
  }

  private fun findOrAddFutureSchedules(requestedSchedules: List<CourseSchedule>, courseActivity: CourseActivity): List<CourseSchedule> =
    requestedSchedules.filter { it.isFutureSchedule() }
      .map { requestedSchedule ->
        courseActivity.courseSchedules.filter { it.isFutureSchedule() }
          .find { savedSchedule -> requestedSchedule matches savedSchedule }
          ?: let { requestedSchedule }
      }
      .toList()

  private fun CourseSchedule.isFutureSchedule() = scheduleDate > LocalDate.now()

  private infix fun CourseSchedule.matches(other: CourseSchedule) =
    scheduleDate == other.scheduleDate &&
      startTime == other.startTime &&
      endTime == other.endTime

  private fun validateRequest(request: SchedulesRequest, courseActivity: CourseActivity) {
    if (request.date < courseActivity.scheduleStartDate) {
      throw BadDataException("Schedule for date ${request.date} is before the activity starts on ${courseActivity.scheduleStartDate}")
    }

    if (request.endTime < request.startTime) {
      throw BadDataException("Schedule for date ${request.date} has times out of order - ${request.startTime} to ${request.endTime}")
    }
  }
}
