package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "property container response")
data class ContainerIdResponse(
  @Schema(description = "The property container id")
  val containerId: Long,
)
