package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location creation response")
data class LocationIdResponse(
  @Schema(description = "The created agency_internal_locations location id")
  val locationId: Long,
)
