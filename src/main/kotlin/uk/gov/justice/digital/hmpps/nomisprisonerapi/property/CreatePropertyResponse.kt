package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prisoner property creation response")
data class CreatePropertyResponse(
  @Schema(description = "The created PROPERTY_CONTAINER_ID", example = "123456789")
  val propertyContainerId: Long,

  @Schema(description = "ID of the booking the property container was added to", example = "123456789")
  val bookingId: Long,
)
