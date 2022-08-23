package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Incentive information")
data class IncentiveResponse(
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "The sequence of the incentive within this booking", required = true)
  val incentiveSequence: Long,
  @Schema(description = "Comment for Incentive level", required = false)
  val commentText: String? = null,
  @Schema(description = "Date and time of Incentive level creation", required = false)
  val iepDateTime: LocalDateTime,
  @Schema(description = "Prison where the Incentive level was created", required = true)
  val prisonId: String,
  @Schema(description = "IEP level code and description", required = true)
  val iepLevel: CodeDescription,
  @Schema(description = "User id of user creating prisoner incentive level", required = false)
  val userId: String? = null,
)
