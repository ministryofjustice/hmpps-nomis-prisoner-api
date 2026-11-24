package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(WarZone.MLTY_WZONE)
class WarZone(code: String, description: String) : ReferenceCode(MLTY_WZONE, code, description) {

  companion object {
    const val MLTY_WZONE = "MLTY_WZONE"
    fun pk(code: String): Pk = Pk(MLTY_WZONE, code)
  }
}
