package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Activity creation response")
data class ActivityResponse(
  @Schema(description = "The created course activity id", required = true)
  @NotNull
  val courseActivityId: Long,

  @Schema(description = "The created course schedules", required = true)
  @NotNull
  val courseSchedules: MutableList<ScheduledInstanceResponse> = mutableListOf(),
) {
  constructor(courseActivity: CourseActivity) : this(courseActivity.courseActivityId) {
    courseActivity.courseSchedules
      .map { ScheduledInstanceResponse(it) }
      .also { this.courseSchedules.addAll(it) }
  }
}

data class ScheduledInstanceResponse(
  @Schema(description = "The created scheduled instance id", required = true)
  @NotNull
  val courseScheduleId: Long,

  @Schema(description = "The instance date", required = true)
  @NotNull
  val date: LocalDate,

  @Schema(description = "The instance start time", required = true)
  @NotNull
  val startTime: LocalTime,

  @Schema(description = "The instance end time", required = true)
  @NotNull
  val endTime: LocalTime,
) {
  constructor(courseSchedule: CourseSchedule) : this(
    courseSchedule.courseScheduleId,
    courseSchedule.scheduleDate,
    courseSchedule.startTime.toLocalTime(),
    courseSchedule.endTime.toLocalTime(),
  )
}
