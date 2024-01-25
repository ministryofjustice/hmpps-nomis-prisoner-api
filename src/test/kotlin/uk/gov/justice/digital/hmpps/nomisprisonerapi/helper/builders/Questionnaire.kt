package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireOffenderRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.QuestionnaireRepository

@DslMarker
annotation class QuestionnaireDslMarker

@NomisDataDslMarker
interface QuestionnaireDsl {
  @QuestionnaireQuestionDslMarker
  fun questionnaireQuestion(
    question: String,
    questionSequence: Int,
    listSequence: Int,
    multipleAnswers: Boolean = false,
    dsl: QuestionnaireQuestionDsl.() -> Unit = {},
  ): QuestionnaireQuestion

  @QuestionnaireOffenderRoleDslMarker
  fun offenderRole(
    role: String,
    dsl: QuestionnaireOffenderRoleDsl.() -> Unit = {},
  ): QuestionnaireOffenderRole
}

@Component
class QuestionnaireBuilderRepository(
  private val repository: QuestionnaireRepository,
) {
  fun save(incidentQuestionnaire: Questionnaire): Questionnaire = repository.save(incidentQuestionnaire)
}

@Component
class QuestionnaireBuilderFactory(
  private val repository: QuestionnaireBuilderRepository,
  private val questionnaireQuestionBuilderFactory: QuestionnaireQuestionBuilderFactory,
  private val questionnaireOffenderRoleBuilderFactory: QuestionnaireOffenderRoleBuilderFactory,
) {
  fun builder() = QuestionnaireBuilder(repository, questionnaireQuestionBuilderFactory, questionnaireOffenderRoleBuilderFactory)
}

class QuestionnaireBuilder(
  private val repository: QuestionnaireBuilderRepository,
  private val questionnaireQuestionBuilderFactory: QuestionnaireQuestionBuilderFactory,
  private val questionnaireOffenderRoleBuilderFactory: QuestionnaireOffenderRoleBuilderFactory,
) : QuestionnaireDsl {
  private lateinit var questionnaire: Questionnaire

  fun build(
    code: String,
    description: String,
    active: Boolean,
    listSequence: Int,
  ): Questionnaire = Questionnaire(
    code = code,
    description = description,
    active = active,
    listSequence = listSequence,
  )
    .let { repository.save(it) }
    .also { questionnaire = it }

  override fun questionnaireQuestion(
    question: String,
    questionSequence: Int,
    listSequence: Int,
    multipleAnswers: Boolean,
    dsl: QuestionnaireQuestionDsl.() -> Unit,
  ): QuestionnaireQuestion =
    questionnaireQuestionBuilderFactory.builder()
      .let { builder ->
        builder.build(
          question = question,
          questionSequence = questionSequence,
          listSequence = listSequence,
          multipleAnswers = multipleAnswers,
        )
          .also { questionnaire.questions += it }
          .also { builder.apply(dsl) }
      }

  override fun offenderRole(
    role: String,
    dsl: QuestionnaireOffenderRoleDsl.() -> Unit,
  ): QuestionnaireOffenderRole =
    questionnaireOffenderRoleBuilderFactory.builder()
      .let { builder ->
        builder.build(
          questionnaireId = questionnaire.id,
          role = role,
        )
          .also { questionnaire.offenderRoles += it }
      }
}
