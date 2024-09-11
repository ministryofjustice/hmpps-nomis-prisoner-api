package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.reconciliation

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Reconciliation details held against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonPersonReconciliationResponse(
  @Schema(description = "The prisoner's unique identifier", example = "A1234AA")
  val offenderNo: String,
  @Schema(description = "The height of the prisoner in centimetres", example = "180")
  val height: Int? = null,
  @Schema(description = "The weight of the prisoner in kilograms", example = "80")
  val weight: Int? = null,
)
