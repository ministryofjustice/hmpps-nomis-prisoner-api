package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(LocalAuthorityType.LOCAL_AUTH)
class LocalAuthorityType(code: String, description: String) : ReferenceCode(LOCAL_AUTH, code, description) {
  companion object {
    const val LOCAL_AUTH = "LOCAL_AUTH"
    fun pk(code: String): Pk = Pk(LOCAL_AUTH, code)
  }
}
