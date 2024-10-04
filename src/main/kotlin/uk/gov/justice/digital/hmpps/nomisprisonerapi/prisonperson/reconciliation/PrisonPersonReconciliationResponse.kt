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
  @Schema(description = "Shape of face", example = "ROUND")
  val face: String? = null,
  @Schema(description = "Build", example = "SLIM")
  val build: String? = null,
  @Schema(description = "Facial hair", example = "CLEAN_SHAVEN")
  val facialHair: String? = null,
  @Schema(description = "Hair colour", example = "BLACK")
  val hair: String? = null,
  @Schema(description = "Left eye colour", example = "BLUE")
  val leftEyeColour: String? = null,
  @Schema(description = "Right eye colour", example = "BROWN")
  val rightEyeColour: String? = null,
  @Schema(description = "Shoe size", example = "8.5")
  val shoeSize: String? = null,
)
