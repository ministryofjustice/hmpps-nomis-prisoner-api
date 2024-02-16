package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location update capacity request")
data class UpdateCapacityRequest(

  @Schema(description = "The maximum physical capacity")
  @field:Min(0, message = "Capacity must be 0 or more")
  val capacity: Int? = null,

  @Schema(description = "The maximum operational capacity")
  @field:Min(0, message = "Operational Capacity must be 0 or more")
  val operationalCapacity: Int? = null,
)
