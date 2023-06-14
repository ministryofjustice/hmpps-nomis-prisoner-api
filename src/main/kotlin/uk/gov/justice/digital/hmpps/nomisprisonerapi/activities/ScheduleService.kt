package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CourseScheduleRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateCourseScheduleResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import java.time.LocalDate
import java.time.LocalTime

@Service
class ScheduleService(
  private val scheduleRepository: CourseScheduleRepository,
  private val activityRepository: CourseActivityRepository,
  private val telemetryClient: TelemetryClient,
) {

  fun mapSchedules(requests: List<CourseScheduleRequest>, courseActivity: CourseActivity): List<CourseSchedule> =
    requests.map {
      validateSchedule(it.date, it.startTime, it.endTime, courseActivity)

      CourseSchedule(
        courseActivity = courseActivity,
        scheduleDate = it.date,
        startTime = it.date.atTime(it.startTime),
        endTime = it.date.atTime(it.endTime),
        slotCategory = SlotCategory.of(it.startTime),
        scheduleStatus = setScheduleStatus(it),
      )
    }.toList()

  private fun setScheduleStatus(it: CourseScheduleRequest) = if (it.cancelled) "CANC" else "SCH"

  fun buildNewSchedules(scheduleRequests: List<CourseScheduleRequest>, courseActivity: CourseActivity): List<CourseSchedule> =
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
        ?.let { requestedSchedule -> savedSchedule.apply { scheduleStatus = requestedSchedule.scheduleStatus } } // TODO SDIT-838 apply changes to date and time - these can change once we start matching by id
        ?: let { throw BadDataException("Cannot update schedules starting before tomorrow") }
    }
      .toList()
  }

  private fun findOrAddFutureSchedules(requestedSchedules: List<CourseSchedule>, courseActivity: CourseActivity): List<CourseSchedule> =
    requestedSchedules.filter { it.isFutureSchedule() }
      .map { requestedSchedule ->
        courseActivity.courseSchedules.filter { it.isFutureSchedule() }
          .find { savedSchedule -> requestedSchedule matches savedSchedule }
          ?.let { savedSchedule -> savedSchedule.apply { scheduleStatus = requestedSchedule.scheduleStatus } }
          ?: let { requestedSchedule }
      }
      .toList()

  private fun CourseSchedule.isFutureSchedule() = scheduleDate > LocalDate.now()

  // TODO SDIT-838 match by id once it is passed on the course schedule request
  private infix fun CourseSchedule.matches(other: CourseSchedule) =
    scheduleDate == other.scheduleDate &&
      startTime == other.startTime &&
      endTime == other.endTime

  private fun validateSchedule(scheduleDate: LocalDate, startTime: LocalTime, endTime: LocalTime, courseActivity: CourseActivity) {
    // TODO SDIT-838 if an id is passed with the course schedule request check it exists on the courseActivity
    if (scheduleDate < courseActivity.scheduleStartDate) {
      throw BadDataException("Schedule for date $scheduleDate is before the activity starts on ${courseActivity.scheduleStartDate}")
    }

    if (endTime < startTime) {
      throw BadDataException("Schedule for date $scheduleDate has times out of order - $startTime to $endTime")
    }
  }

  @Transactional
  fun updateCourseSchedule(
    courseActivityId: Long,
    updateRequest: CourseScheduleRequest,
  ): UpdateCourseScheduleResponse {
    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")

    // TODO SDIT-838 when the id is passed then find by id and error if not found - this is for updates only
    val schedule = with(updateRequest) {
      scheduleRepository.findByCourseActivityAndScheduleDateAndStartTimeAndEndTime(
        courseActivity,
        date,
        date.atTime(startTime),
        date.atTime(endTime),
      )
        ?.apply { scheduleStatus = setScheduleStatus(updateRequest) }
        ?: throw NotFoundException("Course schedule for activity id=$courseActivityId, date=$date, startTime=$startTime, endTime=$endTime not found")
    }

    telemetryClient.trackEvent(
      "activity-course-schedule-updated",
      mapOf(
        "nomisCourseScheduleId" to schedule.courseScheduleId.toString(),
        "nomisCourseActivityId" to courseActivityId.toString(),
      ),
      null,
    )

    return UpdateCourseScheduleResponse(schedule.courseScheduleId)
  }

  fun buildUpdateTelemetry(savedSchedules: List<CourseSchedule>, newSchedules: List<CourseSchedule>): Map<String, String> {
    val removedSchedules = savedSchedules.map { it.courseScheduleId } - newSchedules.map { it.courseScheduleId }.toSet()
    val createdSchedules = newSchedules.map { it.courseScheduleId } - savedSchedules.map { it.courseScheduleId }.toSet()
    val telemetry = mutableMapOf<String, String>()
    // TODO SDIT-838 publish an "updated-courseScheduleIds" event for schedules that have changed
    if (removedSchedules.isNotEmpty()) {
      telemetry["removed-courseScheduleIds"] = removedSchedules.toString()
    }
    if (createdSchedules.isNotEmpty()) {
      telemetry["created-courseScheduleIds"] = createdSchedules.toString()
    }
    return telemetry
  }
}
