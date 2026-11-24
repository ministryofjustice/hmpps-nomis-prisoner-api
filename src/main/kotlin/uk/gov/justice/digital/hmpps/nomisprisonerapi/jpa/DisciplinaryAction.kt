package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(DisciplinaryAction.MLTY_DISCP)
class DisciplinaryAction(code: String, description: String) : ReferenceCode(MLTY_DISCP, code, description) {

  companion object {
    const val MLTY_DISCP = "MLTY_DISCP"
    fun pk(code: String): Pk = Pk(MLTY_DISCP, code)
  }
}
