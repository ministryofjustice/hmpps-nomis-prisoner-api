package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Incentive creation response")
data class CreateIncentiveResponse(
  @Schema(description = "The created Nomis booking and sequence", required = true)
  @NotNull
  val bookingId: Long,
  val sequence: Long,
)
