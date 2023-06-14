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

@Service
class ScheduleService(
  private val scheduleRepository: CourseScheduleRepository,
  private val activityRepository: CourseActivityRepository,
  private val telemetryClient: TelemetryClient,
) {

  fun mapSchedules(requests: List<CourseScheduleRequest>, courseActivity: CourseActivity): List<CourseSchedule> =
    requests.map {
      it.validate(courseActivity)

      CourseSchedule(
        courseScheduleId = it.id ?: 0,
        courseActivity = courseActivity,
        scheduleDate = it.date,
        startTime = it.date.atTime(it.startTime),
        endTime = it.date.atTime(it.endTime),
        slotCategory = SlotCategory.of(it.startTime),
        scheduleStatus = it.getScheduleStatus(),
      )
    }.toList()

  private fun CourseScheduleRequest.getScheduleStatus() = if (cancelled) "CANC" else "SCH"

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
        .find { requestedSchedule -> requestedSchedule.courseScheduleId == savedSchedule.courseScheduleId }
        ?.let { requestedSchedule -> savedSchedule.update(requestedSchedule) }
        ?: let { throw BadDataException("Cannot delete schedules starting before tomorrow") }
    }
      .toList()
  }

  private fun findOrAddFutureSchedules(requestedSchedules: List<CourseSchedule>, courseActivity: CourseActivity): List<CourseSchedule> =
    requestedSchedules.filter { it.isFutureSchedule() }
      .map { requestedSchedule ->
        courseActivity.courseSchedules.filter { it.isFutureSchedule() }
          .find { savedSchedule -> requestedSchedule.courseScheduleId == savedSchedule.courseScheduleId }
          ?.update(requestedSchedule)
          ?: let { requestedSchedule }
      }
      .toList()

  private fun CourseSchedule.isFutureSchedule() = scheduleDate > LocalDate.now()

  private fun CourseSchedule.update(requested: CourseSchedule): CourseSchedule {
    checkForIllegalUpdate(requested)
    return this.apply {
      scheduleDate = requested.scheduleDate
      startTime = requested.startTime
      endTime = requested.endTime
      slotCategory = requested.slotCategory
      scheduleStatus = requested.scheduleStatus
    }
  }

  private fun CourseSchedule.checkForIllegalUpdate(requested: CourseSchedule) {
    if (scheduleDate <= LocalDate.now() &&
      (
        scheduleDate != requested.scheduleDate ||
          startTime != requested.startTime ||
          endTime != requested.endTime
        )
    ) {
      throw BadDataException("Cannot update schedules starting before tomorrow")
    }
  }

  private fun CourseScheduleRequest.validate(courseActivity: CourseActivity) {
    if (id != null && id > 0) {
      courseActivity.courseSchedules.find { it.courseScheduleId == id }
        ?: throw BadDataException("Course schedule $id does not exist")
    }

    if (date < courseActivity.scheduleStartDate) {
      throw BadDataException("Schedule for date $date is before the activity starts on ${courseActivity.scheduleStartDate}")
    }

    if (endTime < startTime) {
      throw BadDataException("Schedule for date $date has times out of order - $startTime to $endTime")
    }
  }

  @Transactional
  fun updateCourseSchedule(
    courseActivityId: Long,
    updateRequest: CourseScheduleRequest,
  ): UpdateCourseScheduleResponse {
    if (!activityRepository.existsById(courseActivityId)) {
      throw NotFoundException("Course activity with id=$courseActivityId does not exist")
    }

    val schedule = scheduleRepository.findByIdOrNull(updateRequest.id)
      ?.update(updateRequest)
      ?: throw NotFoundException("Course schedule id=${updateRequest.id} not found")

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

  private fun CourseSchedule.update(requested: CourseScheduleRequest): CourseSchedule {
    checkForIllegalUpdate(requested)
    return this.apply {
      scheduleDate = requested.date
      startTime = requested.date.atTime(requested.startTime)
      endTime = requested.date.atTime(requested.endTime)
      slotCategory = SlotCategory.of(requested.startTime)
      scheduleStatus = requested.getScheduleStatus()
    }
  }

  private fun CourseSchedule.checkForIllegalUpdate(requested: CourseScheduleRequest) {
    if (scheduleDate <= LocalDate.now() &&
      (
        scheduleDate != requested.date ||
          startTime != requested.date.atTime(requested.startTime) ||
          endTime != requested.date.atTime(requested.endTime)
        )
    ) {
      throw BadDataException("Cannot update schedules starting before tomorrow")
    }
  }

  fun buildUpdateTelemetry(savedSchedules: List<CourseSchedule>, newSchedules: List<CourseSchedule>): Map<String, String> {
    val removedSchedules = savedSchedules.map { it.courseScheduleId } - newSchedules.map { it.courseScheduleId }.toSet()
    val createdSchedules = newSchedules.map { it.courseScheduleId } - savedSchedules.map { it.courseScheduleId }.toSet()
    val updatedSchedules = savedSchedules.findUpdatedSchedules(newSchedules)
    val telemetry = mutableMapOf<String, String>()
    if (removedSchedules.isNotEmpty()) {
      telemetry["removed-courseScheduleIds"] = removedSchedules.toString()
    }
    if (createdSchedules.isNotEmpty()) {
      telemetry["created-courseScheduleIds"] = createdSchedules.toString()
    }
    if (updatedSchedules.isNotEmpty()) {
      telemetry["updated-courseScheduleIds"] = updatedSchedules.toString()
    }
    return telemetry
  }

  private fun List<CourseSchedule>.findUpdatedSchedules(newSchedules: List<CourseSchedule>): List<Long> =
    mapNotNull { savedSchedule ->
      newSchedules
        .find { newSchedule -> newSchedule.courseScheduleId == savedSchedule.courseScheduleId }
        ?.takeIf { newSchedule -> savedSchedule.isChanged(newSchedule) }
    }
      .map { it.courseScheduleId }

  private fun CourseSchedule.isChanged(other: CourseSchedule) =
    scheduleDate != other.scheduleDate ||
      startTime != other.startTime ||
      endTime != other.endTime ||
      scheduleStatus != other.scheduleStatus
}
