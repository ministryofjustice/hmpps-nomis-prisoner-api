package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Provides the generated hearing ID after creation")
data class CreateHearingResponse(

  @Schema(
    description = "The Id for the created Hearing",
    required = true,
  )
  val hearingId: Long,
)
