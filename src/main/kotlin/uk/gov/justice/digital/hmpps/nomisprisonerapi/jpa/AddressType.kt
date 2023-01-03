package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AddressType.ADDR_TYPE)
class AddressType(code: String, description: String) : ReferenceCode(ADDR_TYPE, code, description) {
  companion object {
    const val ADDR_TYPE = "ADDR_TYPE"
    fun pk(code: String): Pk = Pk(ADDR_TYPE, code)
  }
}
