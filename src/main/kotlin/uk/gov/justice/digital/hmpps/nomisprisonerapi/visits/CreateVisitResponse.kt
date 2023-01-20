package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation response")
data class CreateVisitResponse(
  @Schema(description = "The created Nomis visit id", required = true)
  @NotNull
  val visitId: Long,
)
