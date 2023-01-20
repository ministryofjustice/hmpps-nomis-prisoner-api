package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit id")
data class VisitRoomCountResponse(
  @Schema(description = "The internal location description")
  val agencyInternalLocationDescription: String,
  @Schema(description = "The room usage count")
  val count: Long,
  @Schema(description = "The prison id")
  val prisonId: String
)
