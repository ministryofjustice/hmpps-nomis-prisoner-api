package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CSIPInvolvement.CSIP_INV)
class CSIPInvolvement(code: String, description: String) : ReferenceCode(CSIP_INV, code, description) {
  companion object {
    const val CSIP_INV = "CSIP_INV"
    fun pk(code: String): Pk = Pk(CSIP_INV, code)
  }
}
