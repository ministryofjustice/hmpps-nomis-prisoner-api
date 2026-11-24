package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MilitaryDischarge.MLTY_DSCHRG)
class MilitaryDischarge(code: String, description: String) : ReferenceCode(MLTY_DSCHRG, code, description) {

  companion object {
    const val MLTY_DSCHRG = "MLTY_DSCHRG"
    fun pk(code: String): Pk = Pk(MLTY_DSCHRG, code)
  }
}
