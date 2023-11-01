package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Provides the generated hearing result composite ID after creation")
data class CreateHearingResultResponse(
  val hearingId: Long,
  val resultSequence: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A list of Hearing result awards deleted (aka punishment) as a side effect of deleting a hearing result")
data class DeleteHearingResultResponse(
  @Schema(description = "a list of awards that were deleted")
  val awardsDeleted: List<HearingResultAwardResponse> = emptyList(),
)
