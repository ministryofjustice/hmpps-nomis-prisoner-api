package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Non association id")
data class NonAssociationIdResponse(
  @Schema(description = "The 1st offender", required = true)
  val offenderNo1: String,
  @Schema(description = "The 2nd offender", required = true)
  val offenderNo2: String,
)
