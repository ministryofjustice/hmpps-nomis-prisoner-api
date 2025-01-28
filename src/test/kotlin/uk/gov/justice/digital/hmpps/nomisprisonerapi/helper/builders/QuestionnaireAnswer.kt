package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireAnswer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion

@DslMarker
annotation class QuestionnaireAnswerDslMarker

@NomisDataDslMarker
interface QuestionnaireAnswerDsl

@Component
class QuestionnaireAnswerBuilderFactory {
  fun builder() = QuestionnaireAnswerBuilder()
}

class QuestionnaireAnswerBuilder : QuestionnaireAnswerDsl {
  private lateinit var questionnaireAnswer: QuestionnaireAnswer

  fun build(
    answer: String,
    answerSequence: Int,
    listSequence: Int,
    nextQuestion: QuestionnaireQuestion?,
  ): QuestionnaireAnswer = QuestionnaireAnswer(
    answerText = answer,
    answerSequence = answerSequence,
    listSequence = listSequence,
    nextQuestion = nextQuestion,
  )
    .also { questionnaireAnswer = it }
}
