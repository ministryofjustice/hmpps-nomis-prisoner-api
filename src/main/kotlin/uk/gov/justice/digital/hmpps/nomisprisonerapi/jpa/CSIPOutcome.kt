package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CSIPOutcome.CSIP_OUT)
class CSIPOutcome(code: String, description: String) : ReferenceCode(CSIP_OUT, code, description) {
  companion object {
    const val CSIP_OUT = "CSIP_OUT"
    fun pk(code: String): Pk = Pk(CSIP_OUT, code)
  }
}
