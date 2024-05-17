package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestionHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentResponseHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentResponseHistoryId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireAnswer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate

@DslMarker
annotation class IncidentResponseHistoryDslMarker

@NomisDataDslMarker
interface IncidentResponseHistoryDsl

@Component
class IncidentResponseHistoryBuilderFactory {
  fun builder() = IncidentResponseHistoryBuilder()
}

class IncidentResponseHistoryBuilder :
  IncidentResponseHistoryDsl {

  fun build(
    incidentQuestionHistory: IncidentQuestionHistory,
    answer: QuestionnaireAnswer?,
    answerHistorySequence: Int,
    comment: String?,
    responseDate: LocalDate?,
    recordingStaff: Staff,
  ): IncidentResponseHistory =
    IncidentResponseHistory(
      id = IncidentResponseHistoryId(incidentQuestionHistory, answerHistorySequence),
      answer = answer,
      comment = comment,
      responseDate = responseDate,
      recordingStaff = recordingStaff,
    )
}
