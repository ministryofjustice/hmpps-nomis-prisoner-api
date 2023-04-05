package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "IEP Global Incentive level create request")
data class CreateGlobalIncentiveRequest(
  @Schema(description = "incentive level code", example = "STD", required = true)
  val code: String,
  @Schema(description = "describes the incentive level", example = "description for STD", required = true)
  val description: String,
  @Schema(description = "active status of the Global Incentive Level", example = "true", required = true)
  val active: Boolean,
)
