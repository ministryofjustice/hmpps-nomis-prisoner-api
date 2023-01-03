package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(VisitOutcomeReason.VISIT_OUTCOME_REASON)
class VisitOutcomeReason(code: String, description: String) : ReferenceCode(VISIT_OUTCOME_REASON, code, description) {
  companion object {
    const val VISIT_OUTCOME_REASON = "MOVE_CANC_RS"
    fun pk(code: String): Pk = Pk(VISIT_OUTCOME_REASON, code)
  }
}
