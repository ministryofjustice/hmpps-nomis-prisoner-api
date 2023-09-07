package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Non-association creation response")
data class CreateNonAssociationResponse(
  @Schema(description = "The created offender_na_details type sequence number", required = true)
  val typeSequence: Int,
)
