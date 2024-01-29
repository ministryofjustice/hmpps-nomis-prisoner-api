package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestionHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

@DslMarker
annotation class IncidentHistoryDslMarker

@NomisDataDslMarker
interface IncidentHistoryDsl {
  @IncidentQuestionHistoryDslMarker
  fun historyQuestion(
    question: QuestionnaireQuestion,
    dsl: IncidentQuestionHistoryDsl.() -> Unit = {},
  ): IncidentQuestionHistory
}

@Component
class IncidentHistoryBuilderFactory(
  private val incidentQuestionHistoryBuilderFactory: IncidentQuestionHistoryBuilderFactory,
) {
  fun builder() = IncidentHistoryBuilder(incidentQuestionHistoryBuilderFactory)
}

class IncidentHistoryBuilder(
  private val incidentQuestionHistoryBuilderFactory: IncidentQuestionHistoryBuilderFactory,

) :
  IncidentHistoryDsl {
  private lateinit var incidentHistory: IncidentHistory

  fun build(
    questionnaire: Questionnaire,
    changeStaff: Staff,
  ): IncidentHistory = IncidentHistory(
    questionnaire = questionnaire,
    incidentChangeStaff = changeStaff,
  )
    .also { incidentHistory = it }

  override fun historyQuestion(
    question: QuestionnaireQuestion,
    dsl: IncidentQuestionHistoryDsl.() -> Unit,
  ): IncidentQuestionHistory =
    incidentQuestionHistoryBuilderFactory.builder()
      .let { builder ->
        builder.build(
          incidentHistory = incidentHistory,
          question = question,
        )
          .also { incidentHistory.questions += it }
          .also { builder.apply(dsl) }
      }
}
