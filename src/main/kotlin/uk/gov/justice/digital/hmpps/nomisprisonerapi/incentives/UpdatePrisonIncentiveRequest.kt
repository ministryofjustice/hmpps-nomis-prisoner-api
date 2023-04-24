package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Incentive level data create request")
data class UpdatePrisonIncentiveRequest(
  @Schema(description = "active status of the Global Incentive Level", example = "true", required = true)
  val active: Boolean,
  @Schema(description = "default on admission", example = "true", required = true)
  val defaultOnAdmission: Boolean,
  @Schema(description = "The number of weekday visits for a convicted prisoner per fortnight", example = "5500", required = false)
  val visitOrderAllowance: Int?,
  @Schema(description = "The number of privileged/weekend visits for a convicted prisoner per 4 weeks", example = "5500", required = false)
  val privilegedVisitOrderAllowance: Int?,
  @Schema(description = "The amount transferred weekly from the private cash account to the spends account for a remand prisoner to use", example = "5500", required = false)
  val remandTransferLimitInPence: Int? = null,
  @Schema(description = "The maximum amount allowed in the spends account for a remand prisoner", example = "5500", required = false)
  val remandSpendLimitInPence: Int? = null,
  @Schema(description = "The amount transferred weekly from the private cash account to the spends account for a convicted prisoner to use", example = "5500", required = false)
  val convictedTransferLimitInPence: Int? = null,
  @Schema(description = "The maximum amount allowed in the spends account for a convicted prisoner", example = "5500", required = false)
  val convictedSpendLimitInPence: Int? = null,
) {
  fun toCreateRequest(levelCode: String): CreatePrisonIncentiveRequest = CreatePrisonIncentiveRequest(
    levelCode = levelCode,
    active = active,
    defaultOnAdmission = defaultOnAdmission,
    visitOrderAllowance = visitOrderAllowance,
    privilegedVisitOrderAllowance = privilegedVisitOrderAllowance,
    remandTransferLimitInPence = remandTransferLimitInPence,
    remandSpendLimitInPence = remandSpendLimitInPence,
    convictedTransferLimitInPence = convictedTransferLimitInPence,
    convictedSpendLimitInPence = convictedSpendLimitInPence,
  )
}
