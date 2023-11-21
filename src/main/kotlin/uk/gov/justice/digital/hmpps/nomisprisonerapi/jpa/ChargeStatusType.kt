package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(ChargeStatusType.CHARGE_STS)
class ChargeStatusType(code: String, description: String) : ReferenceCode(CHARGE_STS, code, description) {
  companion object {
    const val CHARGE_STS = "CHARGE_STS"
    fun pk(code: String): Pk = Pk(CHARGE_STS, code)
  }
}
