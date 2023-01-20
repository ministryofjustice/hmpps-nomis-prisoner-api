package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "OffenderProgramProfile creation response")
data class CreateOffenderProgramProfileResponse(
  @Schema(description = "The created OffenderProgramProfile id", required = true)
  @NotNull
  val offenderProgramReferenceId: Long,
)
