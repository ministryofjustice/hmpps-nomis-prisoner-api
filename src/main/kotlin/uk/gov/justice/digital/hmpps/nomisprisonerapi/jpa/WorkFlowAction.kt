package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(WorkFlowAction.DOMAIN)
class WorkFlowAction(code: String, description: String) : ReferenceCode(DOMAIN, code, description) {

  companion object {
    const val DOMAIN = "WRK_FLW_ACT"
    const val DataEntry = "ENT"
    const val Verification = "VER"
    const val Modified = "MOD"
    fun pk(code: String): Pk = Pk(DOMAIN, code)
  }
}
