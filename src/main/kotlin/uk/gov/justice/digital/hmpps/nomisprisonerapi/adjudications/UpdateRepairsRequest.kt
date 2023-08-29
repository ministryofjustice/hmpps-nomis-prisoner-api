package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Repairs required due to damage. Any items not in this list will be removed from the Adjudication in NOMIS")
data class UpdateRepairsRequest(
  @Schema(
    description = "Current list of repairs required due to damage",
  ) val repairs: List<RepairToUpdateOrAdd>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RepairToUpdateOrAdd(
  @Schema(
    description = "NOMIS repair type code",
    allowableValues = [
      "CLEA",
      "DECO",
      "ELEC",
      "FABR",
      "LOCK",
      "PLUM",
    ],
  ) val typeCode: String,
  @Schema(
    description = "Description of repair required by damage",
  ) val comment: String?,
)
