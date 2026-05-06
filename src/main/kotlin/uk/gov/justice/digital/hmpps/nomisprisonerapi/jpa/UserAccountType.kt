package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(UserAccountType.USER_AC_TYPE)
class UserAccountType(code: String, description: String) : ReferenceCode(USER_AC_TYPE, code, description) {
  companion object {
    const val USER_AC_TYPE = "USER_AC_TYPE"
    fun pk(code: String): Pk = Pk(USER_AC_TYPE, code)

    const val GENERAL = "GENERAL"
  }
}
