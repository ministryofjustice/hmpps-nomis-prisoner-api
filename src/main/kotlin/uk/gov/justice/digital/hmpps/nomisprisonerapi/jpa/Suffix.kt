package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(Suffix.SUFFIX)
class Suffix(code: String, description: String) : ReferenceCode(SUFFIX, code, description) {
  companion object {
    const val SUFFIX = "SUFFIX"
    fun pk(code: String): Pk = Pk(SUFFIX, code)
  }
}
