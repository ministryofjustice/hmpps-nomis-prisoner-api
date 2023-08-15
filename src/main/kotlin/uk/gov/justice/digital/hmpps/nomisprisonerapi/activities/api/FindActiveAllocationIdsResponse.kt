package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Find active allocation ids response")
data class FindActiveAllocationIdsResponse(
  @Schema(description = "Allocation id", example = "1")
  val allocationId: Long,
)
