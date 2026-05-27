package uk.gov.justice.digital.hmpps.nomisprisonerapi.staff

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Staff id")
data class StaffIdResponse(
  @Schema(description = "The staff id", required = true)
  val staffId: Long,
)
