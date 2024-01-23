package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestionId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion

@DslMarker
annotation class IncidentQuestionDslMarker

@NomisDataDslMarker
interface IncidentQuestionDsl {
  /*
  @IncidentResponseDslMarker
  fun response(
    incident: Incident,
    question: QuestionnaireQuestion,
    answer: QuestionnaireAnswer,
    answerSequence: Int,
    comment: String? = null,
    dsl: IncidentResponseDsl.() -> Unit = {},
  ): IncidentResponse
   */
}

@Component
class IncidentQuestionBuilderFactory(
  // private val repository: IncidentResponseBuilderFactory,
) {
  fun builder() =
    IncidentQuestionBuilder()
  // IncidentQuestionBuilder(repository)
}

class IncidentQuestionBuilder(
  // private val incidentResponseBuilderFactory: IncidentResponseBuilderFactory,

) :
  IncidentQuestionDsl {
  private lateinit var incidentQuestion: IncidentQuestion

  fun build(
    incident: Incident,
    questionSequence: Int,
    question: QuestionnaireQuestion,
  ): IncidentQuestion = IncidentQuestion(
    id = IncidentQuestionId(incident.id, questionSequence),
    incident = incident,
    question = question,
  )
    .also { incidentQuestion = it }

  /*
  override fun response(
    incident: Incident,
    question: QuestionnaireQuestion,
    answer: QuestionnaireAnswer,
    answerSequence: Int,
    comment: String?,
    dsl: IncidentResponseDsl.() -> Unit,
  ): IncidentResponse =
    incidentResponseBuilderFactory.builder()
      .let { builder ->
        builder.build(
          incident = incident,
          question = question,
          answerSequence = answerSequence,
          answer = answer,
          comment = comment,
        )
          // .also { incidentQuestion.responses += it }
          .also { builder.apply(dsl) }
      }

   */
}
