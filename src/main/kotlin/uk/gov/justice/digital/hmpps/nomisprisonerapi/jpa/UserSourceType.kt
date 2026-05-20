package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(UserSourceType.ID_SOURCE)
class UserSourceType(code: String, description: String) : ReferenceCode(ID_SOURCE, code, description) {
  companion object {
    const val ID_SOURCE = "ID_SOURCE"
    fun pk(code: String): Pk = Pk(ID_SOURCE, code)
    val SEQ = UserSourceType("SEQ", "System Sequence")
    val USER = UserSourceType("USER", "User Assigned")
  }
}
