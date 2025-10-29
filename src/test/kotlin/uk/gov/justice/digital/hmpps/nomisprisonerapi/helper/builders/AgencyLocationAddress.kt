package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class AgencyLocationAddressDslMarker

@NomisDataDslMarker
interface AgencyLocationAddressDsl {

  @AddressPhoneDslMarker
  fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: AddressPhoneDsl.() -> Unit = {},
  ): AddressPhone
}

@Component
class AgencyLocationAddressBuilderFactory(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,
  private val agencyAddressBuilderRepository: AgencyLocationAddressBuilderRepository,

) {
  fun builder() = AgencyLocationAddressBuilder(
    addressPhoneBuilderFactory = addressPhoneBuilderFactory,
    agencyAddressBuilderRepository = agencyAddressBuilderRepository,
  )
}

@Component
class AgencyLocationAddressBuilderRepository(
  private val addressTypeRepository: ReferenceCodeRepository<AddressType>,
  private val cityRepository: ReferenceCodeRepository<City>,
  private val countyRepository: ReferenceCodeRepository<County>,
  private val countryRepository: ReferenceCodeRepository<Country>,
  private val addressRepository: AddressRepository,
) {
  fun addressTypeOf(code: String?): AddressType? = code?.let { addressTypeRepository.findByIdOrNull(AddressType.pk(code)) }
  fun cityOf(code: String?): City? = code?.let { cityRepository.findByIdOrNull(City.pk(code)) }
  fun countyOf(code: String?): County? = code?.let { countyRepository.findByIdOrNull(County.pk(code)) }
  fun countryOf(code: String?): Country? = code?.let { countryRepository.findByIdOrNull(Country.pk(code)) }
  fun save(address: AgencyLocationAddress): AgencyLocationAddress = addressRepository.saveAndFlush(address)
}

class AgencyLocationAddressBuilder(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,
  private val agencyAddressBuilderRepository: AgencyLocationAddressBuilderRepository,
) : AgencyLocationAddressDsl {

  private lateinit var address: AgencyLocationAddress

  fun build(
    agencyLocation: AgencyLocation,
    type: String?,
    premise: String?,
    street: String?,
    locality: String?,
    flat: String?,
    postcode: String?,
    city: String?,
    county: String?,
    country: String?,
    validatedPAF: Boolean,
    noFixedAddress: Boolean?,
    primaryAddress: Boolean,
    mailAddress: Boolean,
    comment: String?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    isServices: Boolean,
    businessHours: String?,
    contactPersonName: String?,
  ): AgencyLocationAddress = AgencyLocationAddress(
    agencyLocation = agencyLocation,
    addressType = agencyAddressBuilderRepository.addressTypeOf(type),
    premise = premise,
    street = street,
    locality = locality,
    flat = flat,
    postalCode = postcode,
    city = agencyAddressBuilderRepository.cityOf(city),
    county = agencyAddressBuilderRepository.countyOf(county),
    country = agencyAddressBuilderRepository.countryOf(country),
    validatedPAF = validatedPAF,
    noFixedAddress = noFixedAddress,
    primaryAddress = primaryAddress,
    mailAddress = mailAddress,
    comment = comment,
    startDate = startDate,
    endDate = endDate,
    isServices = isServices,
    businessHours = businessHours,
    contactPersonName = contactPersonName,
  ).also { address = it }

  override fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: AddressPhoneDsl.() -> Unit,
  ): AddressPhone = addressPhoneBuilderFactory.builder().let { builder ->
    builder.build(
      address = address,
      phoneType = phoneType,
      extNo = extNo,
      phoneNo = phoneNo,
      whenCreated = whenCreated,
      whoCreated = whoCreated,
    )
      .also { address.phones += it }
      .also { builder.apply(dsl) }
  }
}
