package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location deactivate request")
data class DeactivateRequest(

  @Schema(
    description = "The reason code for deactivation, reference data 'LIV_UN_RSN'",
    allowableValues = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"],
  )
  val reasonCode: String? = null,
)
