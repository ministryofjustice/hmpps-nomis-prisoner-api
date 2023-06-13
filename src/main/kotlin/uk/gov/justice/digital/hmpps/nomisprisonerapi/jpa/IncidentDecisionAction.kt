package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(IncidentDecisionAction.INC_DECISION)
class IncidentDecisionAction(code: String, description: String) : ReferenceCode(INC_DECISION, code, description) {

  companion object {
    const val INC_DECISION = "INC_DECISION"
    const val NO_FURTHER_ACTION_CODE = "NFA"
    const val PLACED_ON_REPORT_ACTION_CODE = "POR"
    fun pk(code: String): Pk = Pk(INC_DECISION, code)
  }
}