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
    answerSequence: Int,
    listSequence: Int,
    nextQuestion: QuestionnaireQuestion? = null,
    dsl: QuestionnaireAnswerDsl.() -> Unit = {},
  ): QuestionnaireAnswer
}

@Component
class QuestionnaireQuestionBuilderFactory(
  private val questionnaireAnswerBuilderFactory: QuestionnaireAnswerBuilderFactory,
) {
  fun builder() = QuestionnaireQuestionBuilder(/*repository,*/ questionnaireAnswerBuilderFactory)
}

class QuestionnaireQuestionBuilder(
  private val questionnaireAnswerBuilderFactory: QuestionnaireAnswerBuilderFactory,
) :
  QuestionnaireQuestionDsl {
  private lateinit var questionnaireQuestion: QuestionnaireQuestion

  fun build(
    question: String,
    listSequence: Int,
    questionSequence: Int,
    multipleAnswers: Boolean,
  ): QuestionnaireQuestion = QuestionnaireQuestion(
    question = question,
    listSequence = listSequence,
    questionSequence = questionSequence,
    multipleAnswers = multipleAnswers,
  )
    .also { questionnaireQuestion = it }

  override fun questionnaireAnswer(
    answer: String,
    answerSequence: Int,
    listSequence: Int,
    nextQuestion: QuestionnaireQuestion?,
    dsl: QuestionnaireAnswerDsl.() -> Unit,
  ): QuestionnaireAnswer =
    questionnaireAnswerBuilderFactory.builder()
      .let { builder ->
        builder.build(
          answer = answer,
          answerSequence = answerSequence,
          listSequence = listSequence,
          nextQuestion = nextQuestion,
        )
          .also { questionnaireQuestion.answers += it }
          .also { builder.apply(dsl) }
      }
}
