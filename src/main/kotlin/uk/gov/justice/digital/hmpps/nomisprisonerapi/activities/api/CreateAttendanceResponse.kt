package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Attendance creation response")
data class CreateAttendanceResponse(
  @Schema(description = "The created attendance event id", required = true)
  @NotNull
  val eventId: Long,

  @Schema(description = "The course schedule id the attendance was created for", required = true)
  @NotNull
  val courseScheduleId: Long,
)
