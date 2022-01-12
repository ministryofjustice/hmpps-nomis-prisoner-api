package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(VisitOrderType.VISIT_ORDER_TYPE)
class VisitOrderType(code: String, description: String) : ReferenceCode(VISIT_ORDER_TYPE, code, description) {
  companion object {
    const val VISIT_ORDER_TYPE = "VIS_ORD_TYPE"
    val SVO = Pk(VISIT_ORDER_TYPE, "SVO")
    fun pk(code: String): Pk {
      return Pk(VISIT_ORDER_TYPE, code)
    }
  }
}
