package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class AgencyAddressDslMarker

@NomisDataDslMarker
interface AgencyAddressDsl {

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
class AgencyAddressBuilderFactory(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,
  private val AgencyAddressBuilderRepository: AgencyAddressBuilderRepository,

) {
  fun builder() = AgencyAddressBuilder(
    addressPhoneBuilderFactory = addressPhoneBuilderFactory,
    AgencyAddressBuilderRepository = AgencyAddressBuilderRepository,
  )
}

@Component
class AgencyAddressBuilderRepository(
  private val addressTypeRepository: ReferenceCodeRepository<AddressType>,
  private val cityRepository: ReferenceCodeRepository<City>,
  private val countyRepository: ReferenceCodeRepository<County>,
  private val countryRepository: ReferenceCodeRepository<Country>,
  private val addressRepository: AddressRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun addressTypeOf(code: String?): AddressType? = code?.let { addressTypeRepository.findByIdOrNull(AddressType.pk(code)) }
  fun cityOf(code: String?): City? = code?.let { cityRepository.findByIdOrNull(City.pk(code)) }
  fun countyOf(code: String?): County? = code?.let { countyRepository.findByIdOrNull(County.pk(code)) }
  fun countryOf(code: String?): Country? = code?.let { countryRepository.findByIdOrNull(Country.pk(code)) }
  fun save(address: AgencyAddress): AgencyAddress = addressRepository.saveAndFlush(address)
  fun updateCreateDatetime(address: Address, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update ADDRESSES set CREATE_DATETIME = ? where ADDRESS_ID = ?", whenCreated, address.addressId)
  }
  fun updateCreateUsername(address: Address, whoCreated: String) {
    jdbcTemplate.update("update ADDRESSES set CREATE_USER_ID = ? where ADDRESS_ID = ?", whoCreated, address.addressId)
  }
}

class AgencyAddressBuilder(
  private val addressPhoneBuilderFactory: AddressPhoneBuilderFactory,
  private val AgencyAddressBuilderRepository: AgencyAddressBuilderRepository,
) : AgencyAddressDsl {

  private lateinit var address: AgencyAddress

  fun build(
    agencyLocationId: String,
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
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): AgencyAddress = AgencyAddress(
    agencyLocationId = agencyLocationId,
    addressType = AgencyAddressBuilderRepository.addressTypeOf(type),
    premise = premise,
    street = street,
    locality = locality,
    flat = flat,
    postalCode = postcode,
    city = AgencyAddressBuilderRepository.cityOf(city),
    county = AgencyAddressBuilderRepository.countyOf(county),
    country = AgencyAddressBuilderRepository.countryOf(country),
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
  ).let { AgencyAddressBuilderRepository.save(it) }
    .also {
      if (whenCreated != null) {
        AgencyAddressBuilderRepository.updateCreateDatetime(it, whenCreated)
      }
      if (whoCreated != null) {
        AgencyAddressBuilderRepository.updateCreateUsername(it, whoCreated)
      }
    }
    .also { address = it }

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
