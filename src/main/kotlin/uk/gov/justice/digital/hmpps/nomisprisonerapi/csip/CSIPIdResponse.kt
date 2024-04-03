package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP id")
data class CSIPIdResponse(
  @Schema(description = "The csip id", required = true)
  val id: Long,
)
