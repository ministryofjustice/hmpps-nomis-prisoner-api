package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Hearing result to be created")
data class CreateHearingResultRequest(

  @Schema(description = "adjudicator username for the hearing record", example = "ASMITH_GEN")
  val adjudicatorUsername: String?,

  @Schema(description = "The offender's plea code on this charge", example = "NOT_GUILTY")
  val pleaFindingCode: String,

  @Schema(description = "Finding code", example = "GUILTY")
  val findingCode: String,
)
