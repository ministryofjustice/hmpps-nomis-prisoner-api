package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(MaritalStatus.MARITAL_STAT)
class MaritalStatus(code: String, description: String) : ReferenceCode(MARITAL_STAT, code, description) {
  companion object {
    const val MARITAL_STAT = "MARITAL_STAT"
    fun pk(code: String): Pk = Pk(MARITAL_STAT, code)
  }
}
