package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Physical attributes to update a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpsertPhysicalAttributesRequest(
  @Schema(description = "Height (cm)")
  val height: Int?,
  @Schema(description = "Weight (kg)")
  val weight: Int?,
)
