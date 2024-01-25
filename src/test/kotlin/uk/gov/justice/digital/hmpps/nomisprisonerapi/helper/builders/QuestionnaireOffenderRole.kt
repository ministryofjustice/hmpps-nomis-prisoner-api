package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireOffenderRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.QuestionnaireOffenderRoleId

@DslMarker
annotation class QuestionnaireOffenderRoleDslMarker

@NomisDataDslMarker
interface QuestionnaireOffenderRoleDsl

@Component
class QuestionnaireOffenderRoleBuilderFactory() {
  fun builder() = QuestionnaireOffenderRoleBuilder()
}

class QuestionnaireOffenderRoleBuilder() :
  QuestionnaireOffenderRoleDsl {

  fun build(
    questionnaireId: Long,
    role: String,
  ): QuestionnaireOffenderRole =
    QuestionnaireOffenderRole(
      QuestionnaireOffenderRoleId(
        questionnaireId = questionnaireId,
        offenderRole = role,
      ),
    )
}
