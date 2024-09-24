package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.profiledetails

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Profile Details to update a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpsertProfileDetailsRequest(
  @Schema(description = "Profile Type")
  val profileType: String,
  @Schema(description = "Profile Code")
  val profileCode: String?,
)
