package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.time.LocalDate

@Entity
@DiscriminatorValue(AgencyAddress.ADDR_TYPE)
class AgencyAddress(
  @Column("OWNER_CODE")
  val agencyLocationId: String,
  premise: String? = null,
  street: String? = null,
  locality: String? = null,
  startDate: LocalDate? = null,
  endDate: LocalDate? = null,
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
  comment: String? = null,
  isServices: Boolean = false,
  businessHours: String? = null,
  contactPersonName: String? = null,
) : Address(
  addressType = addressType,
  premise = premise,
  street = street,
  locality = locality,
  startDate = startDate,
  endDate = endDate,
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
  comment = comment,
  isServices = isServices,
  businessHours = businessHours,
  contactPersonName = contactPersonName,
) {
  companion object {
    const val ADDR_TYPE = "AGY"
  }
}
