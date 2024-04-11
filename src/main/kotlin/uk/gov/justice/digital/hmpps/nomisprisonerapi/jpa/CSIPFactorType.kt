package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CSIPFactorType.CSIP_FAC)
class CSIPFactorType(code: String, description: String) : ReferenceCode(CSIP_FAC, code, description) {
  companion object {
    const val CSIP_FAC = "CSIP_FAC"
    fun pk(code: String): Pk = Pk(CSIP_FAC, code)
  }
}
