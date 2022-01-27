package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(VisitOrderType.VISIT_ORDER_TYPE)
class VisitOrderType(code: String, description: String) : ReferenceCode(VISIT_ORDER_TYPE, code, description) {
  fun isPrivileged() = code == "PVO"

  companion object {
    const val VISIT_ORDER_TYPE = "VIS_ORD_TYPE"
    fun pk(code: String): Pk = Pk(VISIT_ORDER_TYPE, code)
  }
}
