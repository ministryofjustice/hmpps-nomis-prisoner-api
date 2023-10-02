package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Evidence associated with adjudication incident. Any items not in this list will be removed from the Adjudication in NOMIS")
data class UpdateEvidenceRequest(
  @Schema(
    description = "Current list of evidence items",
  ) val evidence: List<EvidenceToUpdateOrAdd>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EvidenceToUpdateOrAdd(
  @Schema(
    description = "Type of evidence",
    example = "PHOTO",
    allowableValues = [
      "BEHAV",
      "DRUGTEST",
      "EVI_BAG",
      "OTHER",
      "PHOTO",
      "VICTIM",
      "WEAP",
      "WITNESS",
    ],
  )
  val typeCode: String,
  @Schema(description = "Description of evidence", example = "Image of damages")
  val detail: String,

)
