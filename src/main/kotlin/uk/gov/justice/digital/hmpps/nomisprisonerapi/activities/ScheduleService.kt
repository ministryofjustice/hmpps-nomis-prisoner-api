package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.SchedulesRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpdateCourseScheduleRequest
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

  @Transactional
  fun updateCourseSchedule(
    courseActivityId: Long,
    updateRequest: UpdateCourseScheduleRequest,
  ): UpdateCourseScheduleResponse {
    val courseActivity = activityRepository.findByIdOrNull(courseActivityId)
      ?: throw NotFoundException("Course activity with id=$courseActivityId does not exist")

    val schedule = with(updateRequest) {
      scheduleRepository.findByCourseActivityAndScheduleDateAndStartTimeAndEndTime(
        courseActivity,
        date,
        date.atTime(startTime),
        date.atTime(endTime),
      )
        ?.apply { scheduleStatus = if (cancelled) "CANC" else "SCH" }
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
}
