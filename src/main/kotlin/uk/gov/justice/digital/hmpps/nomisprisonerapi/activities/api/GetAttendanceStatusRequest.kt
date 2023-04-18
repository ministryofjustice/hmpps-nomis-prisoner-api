package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Get attendance status request")
data class GetAttendanceStatusRequest(

  @Schema(description = "The date of the course schedule", example = "2023-04-03", required = true)
  val scheduleDate: LocalDate,

  @Schema(description = "The time of the course schedule", example = "10:00", required = true)
  val startTime: LocalTime,

  @Schema(description = "The time the course schedule ends", example = "11:00", required = true)
  val endTime: LocalTime,
)