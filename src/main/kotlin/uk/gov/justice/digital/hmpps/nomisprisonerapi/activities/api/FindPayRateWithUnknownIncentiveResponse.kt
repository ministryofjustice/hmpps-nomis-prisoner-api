package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Find activities with a pay rate with unknown incentive level")
data class FindPayRateWithUnknownIncentiveResponse(
  @Schema(description = "Course description", example = "Kitchens AM")
  val courseActivityDescription: String,
  @Schema(description = "Course Activity ID", example = "1234567")
  val courseActivityId: Long,
  @Schema(description = "Pay band code", example = "5")
  val payBandCode: String,
  @Schema(description = "Incentive level", example = "STD")
  val incentiveLevelCode: String,
)
