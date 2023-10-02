package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import io.swagger.v3.oas.annotations.media.Schema

class UpdateEvidenceResponse(
  @Schema(description = "The evidence associated with the adjudication incident")
  val evidence: List<Evidence>,
)
