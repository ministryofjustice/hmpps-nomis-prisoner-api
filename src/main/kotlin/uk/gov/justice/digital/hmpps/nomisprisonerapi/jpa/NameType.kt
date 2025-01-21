package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(NameType.NAME_TYPE)
class NameType(code: String, description: String) : ReferenceCode(NAME_TYPE, code, description) {
  companion object {
    const val NAME_TYPE = "NAME_TYPE"
    fun pk(code: String): Pk = Pk(NAME_TYPE, code)
  }
}
