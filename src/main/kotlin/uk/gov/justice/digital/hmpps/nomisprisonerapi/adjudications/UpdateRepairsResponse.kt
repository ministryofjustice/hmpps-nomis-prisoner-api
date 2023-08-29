package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import io.swagger.v3.oas.annotations.media.Schema

class UpdateRepairsResponse(
  @Schema(description = "The repairs required due to the damage")
  val repairs: List<Repair>,
)
