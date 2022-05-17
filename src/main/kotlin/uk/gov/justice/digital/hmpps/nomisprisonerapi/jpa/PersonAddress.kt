package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import java.time.LocalDate
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.FetchType.LAZY
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

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
  phones: MutableList<AddressPhone> = mutableListOf()
) : Address(
  premise = premise,
  street = street,
  locality = locality,
  startDate = startDate,
  noFixedAddressFlag = noFixedAddressFlag,
  phones = phones
) {
  companion object {
    const val ADDR_TYPE = "PER"
  }
}
