package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A list of Hearing result awards created (aka punishment)")
data class CreateHearingResultAwardResponses(

  @Schema(description = "an ordered list of award response, the order matching the request order")
  val awardResponses: List<HearingResultAwardResponse>,
)
