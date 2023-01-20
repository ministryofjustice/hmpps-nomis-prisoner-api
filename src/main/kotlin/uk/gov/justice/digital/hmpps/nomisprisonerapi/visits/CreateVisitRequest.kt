package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation request")
data class CreateVisitRequest(

  @Schema(description = "Visit start date and time", required = true)
  val startDateTime: LocalDateTime,

  @Schema(description = "Visit end time", required = true, type = "string", pattern = "HH:mm", example = "14:30")
  val endTime: LocalTime,

  @Schema(description = "Prison where the visit is to occur", required = true)
  val prisonId: String,

  @Schema(description = "Visitors", required = true)
  val visitorPersonIds: List<Long>,

  @Schema(description = "Visit type, whether social or official", allowableValues = ["SCON", "OFFI"], required = true)
  val visitType: String,

  @Schema(description = "Issue date", required = true)
  val issueDate: LocalDate,

  @Schema(description = "Comment to be added to visit")
  val visitComment: String = "Created by VSIP",

  @Schema(description = "Comment to be added to visit order (if one is created)")
  val visitOrderComment: String = "Created by VSIP",

  @Schema(description = "Name of the real world room where visit will take place")
  val room: String,

  @Schema(description = "Whether visit is restricted to a closed session", allowableValues = ["OPEN", "CLOSED"])
  val openClosedStatus: String,
)
