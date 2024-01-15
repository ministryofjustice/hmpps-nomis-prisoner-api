package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Questionnaire id")
data class QuestionnaireIdResponse(
  @Schema(description = "The questionnaire id", required = true)
  val questionnaireId: Long,
)
