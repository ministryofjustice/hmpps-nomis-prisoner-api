package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CSIPInterviewRole.CSIP_INTVROL)
class CSIPInterviewRole(code: String, description: String) : ReferenceCode(CSIP_INTVROL, code, description) {
  companion object {
    const val CSIP_INTVROL = "CSIP_INTVROL"
    fun pk(code: String): Pk = Pk(CSIP_INTVROL, code)
  }
}
