package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Provides the generated Hearing Result Award composite ID after creation")
data class CreateHearingResultAwardResponse(
  val bookingId: Long,
  val sanctionSequence: Int,
)
