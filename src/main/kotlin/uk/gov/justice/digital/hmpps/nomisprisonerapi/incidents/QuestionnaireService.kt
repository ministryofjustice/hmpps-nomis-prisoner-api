package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.QuestionnaireRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.QuestionnaireSpecification

@Service
@Transactional
class QuestionnaireService(
  private val questionnaireRepository: QuestionnaireRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getQuestionnaire(questionnaireId: Long): QuestionnaireResponse = questionnaireRepository.findByIdOrNull(questionnaireId)?.let {
    return toQuestionnaireResponse(it)
  } ?: throw NotFoundException("Questionnaire with id=$questionnaireId does not exist")

  fun findIdsByFilter(pageRequest: Pageable, questionnaireFilter: QuestionnaireFilter): Page<QuestionnaireIdResponse> {
    log.info("Questionnaire Id filter request : $questionnaireFilter with page request $pageRequest")
    return questionnaireRepository.findAll(QuestionnaireSpecification(questionnaireFilter), pageRequest)
      .map { QuestionnaireIdResponse(it.id) }
  }

  private fun toQuestionnaireResponse(entity: Questionnaire): QuestionnaireResponse = QuestionnaireResponse(
    id = entity.id,
    description = entity.description,
    code = entity.code,
    active = entity.active,
    listSequence = entity.listSequence,
    createdBy = entity.createUsername,
    createdDate = entity.createDatetime,
    offenderRoles = entity.offenderRoles.map { it.id.offenderRole },
    questions = entity.questions.map { it.toQuestionResponse() },
  )

  private fun QuestionnaireQuestion.toQuestionResponse(): QuestionResponse = QuestionResponse(
    id = id,
    question = questionText,
    active = active,
    questionSequence = questionSequence,
    listSequence = listSequence,
    multipleAnswers = multipleAnswers,
    answers = answers.map { questionnaireAnswer ->
      AnswerResponse(
        id = questionnaireAnswer.id,
        answer = questionnaireAnswer.answerText,
        active = questionnaireAnswer.active,
        answerSequence = questionnaireAnswer.answerSequence,
        listSequence = questionnaireAnswer.listSequence,
        commentRequired = questionnaireAnswer.commentRequired,
        dateRequired = questionnaireAnswer.dateRequired,
      )
        .apply {
          this.nextQuestion = questionnaireAnswer.nextQuestion?.toNextQuestionResponse()
        }
    },
  )

  private fun QuestionnaireQuestion.toNextQuestionResponse(): NextQuestionResponse = NextQuestionResponse(
    id = id,
    question = questionText,
  )
}
