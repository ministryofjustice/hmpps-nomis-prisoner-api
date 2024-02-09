package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(WorkFlowAction.DOMAIN)
class WorkFlowAction(code: String, description: String) : ReferenceCode(DOMAIN, code, description) {

  companion object {
    const val DOMAIN = "WRK_FLW_ACT"
    const val DATA_ENTRY = "ENT"
    const val VERIFICATION = "VER"
    const val MODIFIED = "MOD"
    fun pk(code: String): Pk = Pk(DOMAIN, code)
  }
}
