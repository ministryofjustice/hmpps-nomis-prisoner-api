package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A list of Hearing result awards created (aka punishment)")
data class CreateHearingResultAwardResponses(
  @Schema(description = "an ordered list of award response, the order matching the request order")
  val awardsCreated: List<HearingResultAwardResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A list of Hearing result awards created (aka punishment)")
data class UpdateHearingResultAwardResponses(
  @Schema(description = "an ordered list of awards created, the order matching the request order for awardRequestsToCreate")
  val awardsCreated: List<HearingResultAwardResponse>,
  @Schema(description = "a list of awards that were deleted due to this update")
  val awardsDeleted: List<HearingResultAwardResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A list of Hearing result awards deleted (aka punishment)")
data class DeleteHearingResultAwardResponses(
  @Schema(description = "a list of awards that were deleted")
  val awardsDeleted: List<HearingResultAwardResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Provides the generated Hearing Result Award composite ID after creation")
data class HearingResultAwardResponse(
  val bookingId: Long,
  val sanctionSequence: Int,
)
