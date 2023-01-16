package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit update request")
data class UpdateVisitRequest(

  @Schema(description = "Visit start date and time", required = true)
  val startDateTime: LocalDateTime,

  @Schema(description = "Visit end time", required = true, type = "string", pattern = "HH:mm", example = "14:30")
  val endTime: LocalTime,

  @Schema(description = "Visitors", required = true)
  val visitorPersonIds: List<Long>,

  @Schema(description = "Name of the real world room where visit will take place")
  val room: String,

  @Schema(description = "Whether visit is restricted to a closed session", allowableValues = ["OPEN", "CLOSED"])
  val openClosedStatus: String,
)
