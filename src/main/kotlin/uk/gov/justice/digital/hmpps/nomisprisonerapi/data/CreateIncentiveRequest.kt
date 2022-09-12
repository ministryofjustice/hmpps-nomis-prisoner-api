package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "IEP creation request")
data class CreateIncentiveRequest(
  @Schema(description = "IEP Level", example = "Standard", required = true)
  val iepLevel: String,
  @Schema(description = "Review comments", example = "A review took place", required = false)
  val comments: String? = null,
  @Schema(description = "Date when last review took place", example = "2022-08-12", required = true)
  val iepDate: LocalDate,
  @Schema(description = "Time when last review took place", required = true)
  val iepTime: LocalTime,
  @Schema(description = "Prison ID", example = "MDI", required = true)
  val agencyId: String,
  @Schema(description = "Username of the reviewer", example = "AJONES", required = true)
  val userId: String?,
)
