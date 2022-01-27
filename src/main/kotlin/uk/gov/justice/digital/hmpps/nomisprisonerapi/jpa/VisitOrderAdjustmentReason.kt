package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(VisitOrderAdjustmentReason.VISIT_ORDER_ADJUSTMENT)
class VisitOrderAdjustmentReason(code: String, description: String) :
  ReferenceCode(VISIT_ORDER_ADJUSTMENT, code, description) {
  companion object {
    const val VISIT_ORDER_ISSUE = "VO_ISSUE"
    const val PRIVILEGED_VISIT_ORDER_ISSUE = "PVO_ISSUE"
    const val VISIT_ORDER_CANCEL = "VO_CANCEL"
    const val PRIVILEGED_VISIT_ORDER_CANCEL = "PVO_CANCEL"

    const val VISIT_ORDER_ADJUSTMENT = "VIS_ORD_ADJ"

    val VO_ISSUE = pk(VISIT_ORDER_ISSUE)
    val PVO_ISSUE = pk(PRIVILEGED_VISIT_ORDER_ISSUE)
    val VO_CANCEL = pk(VISIT_ORDER_CANCEL)
    val PVO_CANCEL = pk(PRIVILEGED_VISIT_ORDER_CANCEL)
    fun pk(code: String): Pk = Pk(VISIT_ORDER_ADJUSTMENT, code)
  }
}
