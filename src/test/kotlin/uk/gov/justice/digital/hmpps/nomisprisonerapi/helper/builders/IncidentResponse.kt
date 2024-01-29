package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentResponseId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireAnswer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

@DslMarker
annotation class IncidentResponseDslMarker

@NomisDataDslMarker
interface IncidentResponseDsl

@Component
class IncidentResponseBuilderFactory {
  fun builder() = IncidentResponseBuilder()
}

class IncidentResponseBuilder :
  IncidentResponseDsl {

  fun build(
    incidentQuestion: IncidentQuestion,
    answer: QuestionnaireAnswer,
    answerSequence: Int,
    comment: String?,
    recordingStaff: Staff,
  ): IncidentResponse =
    IncidentResponse(
      id = IncidentResponseId(incidentQuestion, answerSequence),
      answer = answer,
      comment = comment,
      recordingStaff = recordingStaff,
    )
}
