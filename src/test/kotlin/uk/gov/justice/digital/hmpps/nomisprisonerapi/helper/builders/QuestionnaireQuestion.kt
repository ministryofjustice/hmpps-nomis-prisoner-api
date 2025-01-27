package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireAnswer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion

@DslMarker
annotation class QuestionnaireQuestionDslMarker

@NomisDataDslMarker
interface QuestionnaireQuestionDsl {
  @QuestionnaireAnswerDslMarker
  fun questionnaireAnswer(
    answer: String,
    nextQuestion: QuestionnaireQuestion? = null,
    dsl: QuestionnaireAnswerDsl.() -> Unit = {},
  ): QuestionnaireAnswer
}

@Component
class QuestionnaireQuestionBuilderFactory(
  private val questionnaireAnswerBuilderFactory: QuestionnaireAnswerBuilderFactory,
) {
  fun builder() = QuestionnaireQuestionBuilder(questionnaireAnswerBuilderFactory)
}

class QuestionnaireQuestionBuilder(
  private val questionnaireAnswerBuilderFactory: QuestionnaireAnswerBuilderFactory,
) : QuestionnaireQuestionDsl {
  private lateinit var questionnaireQuestion: QuestionnaireQuestion

  fun build(
    question: String,
    questionSequence: Int,
    listSequence: Int,
    multipleAnswers: Boolean,
  ): QuestionnaireQuestion = QuestionnaireQuestion(
    questionText = question,
    listSequence = listSequence,
    questionSequence = questionSequence,
    multipleAnswers = multipleAnswers,
  )
    .also { questionnaireQuestion = it }

  override fun questionnaireAnswer(
    answer: String,
    nextQuestion: QuestionnaireQuestion?,
    dsl: QuestionnaireAnswerDsl.() -> Unit,
  ): QuestionnaireAnswer = questionnaireAnswerBuilderFactory.builder()
    .let { builder ->
      builder.build(
        answer = answer,
        answerSequence = questionnaireQuestion.answers.size,
        listSequence = questionnaireQuestion.answers.size,
        nextQuestion = nextQuestion,
      )
        .also { questionnaireQuestion.answers += it }
        .also { builder.apply(dsl) }
    }
}
