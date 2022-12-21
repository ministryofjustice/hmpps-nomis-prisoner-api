package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation response")
data class CreateIncentiveResponse(
  @Schema(description = "The created Nomis id", required = true)
  @NotNull
  val bookingId: Long,
  val sequence: Long,
)
