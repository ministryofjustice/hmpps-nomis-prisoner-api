package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "IEP creation request")
data class CreateIncentiveRequest(
  @Schema(description = "IEP Level", example = "Standard", required = true)
  val iepLevel: String,
  @Schema(description = "Review comments", example = "A review took place", required = false)
  val comments: String? = null,
  @Schema(description = "Date and time when last review took place", example = "2022-08-12T14:30", required = true)
  val iepDateTime: LocalDateTime,
  @Schema(description = "Prison ID", example = "MDI", required = true)
  val prisonId: String,
  @Schema(description = "Username of the reviewer", example = "AJONES", required = true)
  val userId: String?,
)
