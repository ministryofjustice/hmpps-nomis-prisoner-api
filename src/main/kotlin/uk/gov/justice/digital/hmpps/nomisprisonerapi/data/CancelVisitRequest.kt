package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit cancellation")
data class CancelVisitRequest(
  // TODO This is a guess, possible values are dependent on VSIP
  @Schema(description = "The cancellation reason", allowableValues = ["VISCANC", "OFFCANC", "ADMIN", "NSHOW"], required = true)
  @NotEmpty
  val outcome: String,
)
