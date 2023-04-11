package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Attendance create/update response")
data class UpsertAttendanceResponse(
  @Schema(description = "The attendance event id", required = true)
  @NotNull
  val eventId: Long,

  @Schema(description = "The course schedule id for the attendance", required = true)
  @NotNull
  val courseScheduleId: Long,

  @Schema(description = "Whether or the attendance was created", required = true)
  @NotNull
  val created: Boolean,
)
