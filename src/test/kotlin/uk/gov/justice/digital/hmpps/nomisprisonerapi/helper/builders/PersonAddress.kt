package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class PersonAddressDslMarker

@NomisDataDslMarker
interface PersonAddressDsl {
  companion object {
    const val SHEFFIELD = "25343"
  }

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
class PersonAddressBuilderFactory(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,
  private val personAddressBuilderRepository: PersonAddressBuilderRepository,

) {
  fun builder() = PersonAddressBuilder(
    addressPhoneBuilderFactory = addressPhoneBuilderFactory,
    personAddressBuilderRepository = personAddressBuilderRepository,
  )
}

@Component
class PersonAddressBuilderRepository(
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
  fun save(address: PersonAddress): PersonAddress = addressRepository.saveAndFlush(address)
}

class PersonAddressBuilder(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,
  private val personAddressBuilderRepository: PersonAddressBuilderRepository,
) : PersonAddressDsl {

  private lateinit var address: PersonAddress

  fun build(
    type: String?,
    person: Person,
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
  ): PersonAddress =
    PersonAddress(
      addressType = personAddressBuilderRepository.addressTypeOf(type),
      person = person,
      premise = premise,
      street = street,
      locality = locality,
      flat = flat,
      postalCode = postcode,
      city = personAddressBuilderRepository.cityOf(city),
      county = personAddressBuilderRepository.countyOf(county),
      country = personAddressBuilderRepository.countryOf(country),
      validatedPAF = validatedPAF,
      noFixedAddress = noFixedAddress,
      primaryAddress = primaryAddress,
      mailAddress = mailAddress,
      comment = comment,
      startDate = startDate,
      endDate = endDate,
    ).let { personAddressBuilderRepository.save(it) }
      .also { address = it }

  override fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: AddressPhoneDsl.() -> Unit,
  ): AddressPhone =
    addressPhoneBuilderFactory.builder().let { builder ->
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
