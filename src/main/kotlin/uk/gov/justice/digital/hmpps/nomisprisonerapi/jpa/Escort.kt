package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Escort.ESCORT)
class Escort(code: String, description: String) : ReferenceCode(ESCORT, code, description) {
  companion object {
    const val ESCORT = "ESCORT"
    fun pk(code: String): Pk = Pk(ESCORT, code)
  }
}
