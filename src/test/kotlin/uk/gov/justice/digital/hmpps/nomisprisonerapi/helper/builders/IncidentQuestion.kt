package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestionId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireAnswer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate

@DslMarker
annotation class IncidentQuestionDslMarker

@NomisDataDslMarker
interface IncidentQuestionDsl {
  @IncidentResponseDslMarker
  fun response(
    answer: QuestionnaireAnswer? = null,
    comment: String? = null,
    responseDate: LocalDate? = null,
    recordingStaff: Staff,
    dsl: IncidentResponseDsl.() -> Unit = {},
  ): IncidentResponse
}

@Component
class IncidentQuestionBuilderFactory(
  private val incidentResponseBuilderFactory: IncidentResponseBuilderFactory,
) {
  fun builder() = IncidentQuestionBuilder(incidentResponseBuilderFactory)
}

class IncidentQuestionBuilder(
  private val incidentResponseBuilderFactory: IncidentResponseBuilderFactory,
) : IncidentQuestionDsl {
  private lateinit var incidentQuestion: IncidentQuestion
  private lateinit var incident: Incident

  fun build(
    incident: Incident,
    questionSequence: Int,
    question: QuestionnaireQuestion,
  ): IncidentQuestion = IncidentQuestion(
    id = IncidentQuestionId(incident.id, questionSequence),
    question = question,
  )
    .also {
      this.incident = incident
      incidentQuestion = it
    }

  override fun response(
    answer: QuestionnaireAnswer?,
    comment: String?,
    responseDate: LocalDate?,
    recordingStaff: Staff,
    dsl: IncidentResponseDsl.() -> Unit,
  ): IncidentResponse = incidentResponseBuilderFactory.builder()
    .let { builder ->
      builder.build(
        incidentQuestion = incidentQuestion,
        answerSequence = incident.questions.sumOf { it.responses.count() },
        answer = answer,
        comment = comment,
        responseDate = responseDate,
        recordingStaff = recordingStaff,
      )
        .also { incidentQuestion.responses += it }
        .also { builder.apply(dsl) }
    }
}
