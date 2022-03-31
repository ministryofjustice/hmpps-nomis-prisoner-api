package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit id")
data class VisitRoomCountResponse(
  @Schema(description = "The internal location description", required = true)
  val agencyInternalLocationDescription: String,
  val count: Long
)
