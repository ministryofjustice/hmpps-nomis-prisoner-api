package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(CSIPSignedOffRole.CSIP_ROLE)
class CSIPSignedOffRole(code: String, description: String) : ReferenceCode(CSIP_ROLE, code, description) {
  companion object {
    const val CSIP_ROLE = "CSIP_ROLE"
    fun pk(code: String): Pk = Pk(CSIP_ROLE, code)
  }
}
