package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestionHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestionHistoryId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentResponseHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireAnswer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate

@DslMarker
annotation class IncidentQuestionHistoryDslMarker

@NomisDataDslMarker
interface IncidentQuestionHistoryDsl {
  @IncidentResponseHistoryDslMarker
  fun historyResponse(
    answer: QuestionnaireAnswer? = null,
    comment: String? = null,
    responseDate: LocalDate? = null,
    recordingStaff: Staff,
    dsl: IncidentResponseHistoryDsl.() -> Unit = {},
  ): IncidentResponseHistory
}

@Component
class IncidentQuestionHistoryBuilderFactory(
  private val incidentResponseHistoryBuilderFactory: IncidentResponseHistoryBuilderFactory,
) {
  fun builder() = IncidentQuestionHistoryBuilder(incidentResponseHistoryBuilderFactory)
}

class IncidentQuestionHistoryBuilder(
  private val incidentResponseHistoryBuilderFactory: IncidentResponseHistoryBuilderFactory,
) : IncidentQuestionHistoryDsl {
  private lateinit var incidentQuestionHistory: IncidentQuestionHistory

  fun build(
    incidentHistory: IncidentHistory,
    question: QuestionnaireQuestion,
  ): IncidentQuestionHistory = IncidentQuestionHistory(
    id = IncidentQuestionHistoryId(incidentHistory, question.questionSequence),
    question = question,
  )
    .also { incidentQuestionHistory = it }

  override fun historyResponse(
    answer: QuestionnaireAnswer?,
    comment: String?,
    responseDate: LocalDate?,
    recordingStaff: Staff,
    dsl: IncidentResponseHistoryDsl.() -> Unit,
  ): IncidentResponseHistory = incidentResponseHistoryBuilderFactory.builder()
    .let { builder ->
      builder.build(
        incidentQuestionHistory = incidentQuestionHistory,
        answerHistorySequence = incidentQuestionHistory.responses.size + 1,
        answer = answer,
        comment = comment,
        responseDate = responseDate,
        recordingStaff = recordingStaff,
      )
        .also { incidentQuestionHistory.responses += it }
        .also { builder.apply(dsl) }
    }
}
