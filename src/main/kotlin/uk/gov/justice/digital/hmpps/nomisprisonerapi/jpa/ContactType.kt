package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(ContactType.CONTACTS)
class ContactType(code: String, description: String) : ReferenceCode(CONTACTS, code, description) {

  companion object {
    const val CONTACTS = "CONTACTS"
    fun pk(code: String): Pk {
      return Pk(CONTACTS, code)
    }
  }
}
