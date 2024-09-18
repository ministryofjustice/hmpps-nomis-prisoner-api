package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate

@Entity
@DiscriminatorValue(PersonAddress.ADDR_TYPE)
class PersonAddress(
  @JoinColumn(name = "OWNER_ID")
  @ManyToOne(optional = false, fetch = LAZY)
  val person: Person,
  premise: String? = null,
  street: String? = null,
  locality: String? = null,
  startDate: LocalDate = LocalDate.now(),
  noFixedAddressFlag: String = "N",
  phones: MutableList<AddressPhone> = mutableListOf(),
  addressType: AddressType? = null,
  flat: String? = null,
  postalCode: String? = null,
) : Address(
  addressType = addressType,
  premise = premise,
  street = street,
  locality = locality,
  startDate = startDate,
  noFixedAddressFlag = noFixedAddressFlag,
  phones = phones,
  flat = flat,
  postalCode = postalCode,
) {
  companion object {
    const val ADDR_TYPE = "PER"
  }
}
