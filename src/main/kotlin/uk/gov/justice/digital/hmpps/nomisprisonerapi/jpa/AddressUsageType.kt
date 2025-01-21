package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(AddressUsageType.ADDRESS_TYPE)
class AddressUsageType(code: String, description: String) : ReferenceCode(ADDRESS_TYPE, code, description) {
  companion object {
    const val ADDRESS_TYPE = "ADDRESS_TYPE"
    fun pk(code: String): Pk = Pk(ADDRESS_TYPE, code)
  }
}
