package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(IdentifierType.ID_TYPE)
class IdentifierType(code: String, description: String) : ReferenceCode(ID_TYPE, code, description) {
  companion object {
    const val ID_TYPE = "ID_TYPE"
    fun pk(code: String): Pk = Pk(ID_TYPE, code)
  }
}
