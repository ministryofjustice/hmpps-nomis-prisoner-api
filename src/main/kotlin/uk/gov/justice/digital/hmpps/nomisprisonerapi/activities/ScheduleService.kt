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

  /*
   * Build a list of schedules to save against the Activity:
   * - any past schedules that are already saved are considered to be immutable, you can't change history
   * - any attempts to change immutable schedules are ignored (which means the request doesn't need to include the full schedule history, we won't delete missing old schedules)
   * - updatable schedules are any saved schedules from today onwards included in the request
   * - schedules from today onwards not included in the request are deleted
   * - unsaved schedules are any schedules included in the request but not yet saved
   */
  fun buildNewSchedules(scheduleRequests: List<CourseScheduleRequest>, courseActivity: CourseActivity): List<CourseSchedule> {
    val immutableSchedules = courseActivity.courseSchedules.filter { it.isImmutable() }
    val immutableIds = immutableSchedules.map { it.courseScheduleId }

    val requestedSchedules = mapSchedules(scheduleRequests.filter { !immutableIds.contains(it.id) }, courseActivity)
    val updatableSchedules = requestedSchedules.mapNotNull {
        req ->
      courseActivity.courseSchedules
        .find { req.courseScheduleId == it.courseScheduleId }
        ?.update(req)
    }

    val unsavedSchedules = requestedSchedules - updatableSchedules.toSet()

    return immutableSchedules + updatableSchedules + unsavedSchedules
  }

  private fun CourseSchedule.isImmutable() = this.scheduleDate < LocalDate.now()

  private fun CourseSchedule.update(requested: CourseSchedule): CourseSchedule {
    return this.apply {
      scheduleDate = requested.scheduleDate
      startTime = requested.startTime
      endTime = requested.endTime
      slotCategory = requested.slotCategory
      scheduleStatus = requested.scheduleStatus
    }
  }

  private fun CourseScheduleRequest.validate(courseActivity: CourseActivity) {
    if (id != null && id > 0) {
      courseActivity.courseSchedules.find { it.courseScheduleId == id }
        ?: throw NotFoundException("Course schedule $id does not exist")
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
    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")

    updateRequest.validate(courseActivity)

    val schedule = scheduleRepository.findByIdOrNull(updateRequest.id)
      ?: throw NotFoundException("Course schedule id=${updateRequest.id} not found")

    if (schedule.isImmutable()) {
      throw BadDataException("Cannot change schedule id=${schedule.courseScheduleId} because it is immutable")
    } else {
      schedule.update(updateRequest)
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

  private fun CourseSchedule.update(requested: CourseScheduleRequest): CourseSchedule {
    return this.apply {
      scheduleDate = requested.date
      startTime = requested.date.atTime(requested.startTime)
      endTime = requested.date.atTime(requested.endTime)
      slotCategory = SlotCategory.of(requested.startTime)
      scheduleStatus = requested.getScheduleStatus()
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
