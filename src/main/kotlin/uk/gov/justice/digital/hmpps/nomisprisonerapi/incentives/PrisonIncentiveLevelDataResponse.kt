package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Incentive information")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonIncentiveLevelDataResponse(
  val prisonId: String,
  val iepLevelCode: String,
  val visitOrderAllowance: Int? = null,
  val privilegedVisitOrderAllowance: Int? = null,
  val defaultOnAdmission: Boolean,
  val remandTransferLimitInPence: Int? = null,
  val remandSpendLimitInPence: Int? = null,
  val convictedTransferLimitInPence: Int? = null,
  val convictedSpendLimitInPence: Int? = null,
  val active: Boolean,
  val expiryDate: LocalDate? = null,
  val visitAllowanceActive: Boolean?,
  val visitAllowanceExpiryDate: LocalDate? = null,
)
