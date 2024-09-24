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
  phones: MutableList<AddressPhone> = mutableListOf(),
  addressType: AddressType? = null,
  flat: String? = null,
  postalCode: String? = null,
  city: City? = null,
  county: County? = null,
  country: Country? = null,
  validatedPAF: Boolean = false,
  noFixedAddress: Boolean? = false,
  primaryAddress: Boolean = false,
  mailAddress: Boolean = false,
) : Address(
  addressType = addressType,
  premise = premise,
  street = street,
  locality = locality,
  startDate = startDate,
  noFixedAddress = noFixedAddress,
  phones = phones,
  flat = flat,
  postalCode = postalCode,
  city = city,
  county = county,
  country = country,
  validatedPAF = validatedPAF,
  primaryAddress = primaryAddress,
  mailAddress = mailAddress,
) {
  companion object {
    const val ADDR_TYPE = "PER"
  }
}
