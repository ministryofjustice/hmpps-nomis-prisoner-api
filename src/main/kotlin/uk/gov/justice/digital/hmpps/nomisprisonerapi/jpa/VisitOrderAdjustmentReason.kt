package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(VisitOrderAdjustmentReason.VISIT_ORDER_ADJUSTMENT)
class VisitOrderAdjustmentReason(code: String, description: String) :
  ReferenceCode(VISIT_ORDER_ADJUSTMENT, code, description) {
  companion object {
    const val VISIT_ORDER_ISSUE = "VO_ISSUE"
    const val PVO_ISSUE = "PVO_ISSUE"
    const val VISIT_ORDER_ADJUSTMENT = "VIS_ORD_ADJ"
    val VO_ISSUE = Pk(VISIT_ORDER_ADJUSTMENT, VISIT_ORDER_ISSUE)
    fun pk(code: String): Pk {
      return Pk(VISIT_ORDER_ADJUSTMENT, code)
    }
  }
}
