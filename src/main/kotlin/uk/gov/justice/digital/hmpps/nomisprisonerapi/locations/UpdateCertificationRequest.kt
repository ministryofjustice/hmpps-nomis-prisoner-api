package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location update certification request")
data class UpdateCertificationRequest(

  @Schema(description = "The CNA certified capacity")
  @field:Min(0, message = "CNA Capacity must be 0 or more")
  val cnaCapacity: Int,

  @Schema(description = "Whether the location is certified")
  val certified: Boolean,
)
