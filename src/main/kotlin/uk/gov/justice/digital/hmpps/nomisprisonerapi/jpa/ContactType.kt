package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(ContactType.CONTACTS)
class ContactType(code: String, description: String) : ReferenceCode(CONTACTS, code, description) {

  companion object {
    const val CONTACTS = "CONTACTS"
    fun pk(code: String): Pk = Pk(CONTACTS, code)
  }
}
