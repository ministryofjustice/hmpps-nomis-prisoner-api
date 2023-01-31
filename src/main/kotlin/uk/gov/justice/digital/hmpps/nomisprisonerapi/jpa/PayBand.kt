package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(PayBand.PAY_BAND)
class PayBand(code: String, description: String) : ReferenceCode(PAY_BAND, code, description) {

  companion object {
    const val PAY_BAND = "PAY_BAND"
    fun pk(code: String): Pk = Pk(PAY_BAND, code)
  }
}
