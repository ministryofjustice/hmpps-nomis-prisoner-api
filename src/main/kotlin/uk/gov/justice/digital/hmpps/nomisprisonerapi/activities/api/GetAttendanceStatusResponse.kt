package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Attendance status response")
data class GetAttendanceStatusResponse(
  @Schema(description = "The event status for the attendance", required = true)
  @NotNull
  val eventStatus: String,
)
