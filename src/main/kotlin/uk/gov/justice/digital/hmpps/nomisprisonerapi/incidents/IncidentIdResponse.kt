package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Incident id")
data class IncidentIdResponse(
  @Schema(description = "The incident id")
  val incidentId: Long,
)
