package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Incentive id")
data class IncentiveIdResponse(
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "The sequence of the incentive within this booking", required = true)
  val sequence: Long,
)
