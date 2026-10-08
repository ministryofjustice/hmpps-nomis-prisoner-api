package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.trackEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternetAddress.Companion.EMAIL_INTERNET_ADDRESS_CLASS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Service
@Transactional
class AgencyService(
  val agencyLocationRepository: AgencyLocationRepository,
  val agencyLocationInternetAddressRepository: AgencyLocationInternetAddressRepository,
  val agencyLocationPhoneRepository: AgencyLocationPhoneRepository,
  val agencyLocationAddressRepository: AgencyLocationAddressRepository,
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,
  private val addressTypeRepository: ReferenceCodeRepository<AddressType>,
  private val cityRepository: ReferenceCodeRepository<City>,
  private val countyRepository: ReferenceCodeRepository<County>,
  private val countryRepository: ReferenceCodeRepository<Country>,
  private val telemetryClient: TelemetryClient,
) {
  fun getAllAgencies(excludeType: List<String>): AgencyIdsResponse {
    val agencies = excludeType.takeIf { it.isNotEmpty() }?.let {
      agencyLocationRepository.findByType_CodeNotIn(it)
    } ?: agencyLocationRepository.findAll()

    // Filter out the "AUHSA;" agency ID can not be used since it has semi-colon and has been replaced by "AUHSA" in NOMIS.
    // All references have also been replaced by "AUHSA" but "AUHSA;" has not been deleted. So just ignore it
    return AgencyIdsResponse(
      agencyIds = agencies.filter { it.id != "AUHSA;" }.map { AgencyId(it.id) },
    )
  }

  fun getAgencyLocation(agencyId: String): AgencyResponse = agencyLocationRepository.findByIdOrNull(agencyId)
    ?.toAgencyLocationResponse() ?: throw NotFoundException("Agency $agencyId does not exist")

  fun createAgencyEmail(agencyId: String, request: CreateAgencyEmailAddressRequest): CreateAgencyEmailAddressResponse = agencyLocationInternetAddressRepository.saveAndFlush(
    AgencyLocationInternetAddress(
      agencyLocation = getAgency(agencyId),
      internetAddress = request.emailAddress,
      internetAddressClass = EMAIL_INTERNET_ADDRESS_CLASS,
    ),
  ).let {
    telemetryClient.trackEvent(
      "agency-email-inserted",
      mapOf(
        "agencyId" to agencyId,
        "emailAddressId" to it.internetAddressId.toString(),
      ),
    )
    CreateAgencyEmailAddressResponse(id = it.internetAddressId)
  }

  fun createAgencyPhone(agencyId: String, request: CreateAgencyPhoneNumberRequest): CreateAgencyPhoneNumberResponse = agencyLocationPhoneRepository.saveAndFlush(
    AgencyLocationPhone(
      agencyLocation = getAgency(agencyId),
      phoneType = phoneTypeOf(request.typeCode),
      phoneNo = request.number,
      extNo = request.extension,
    ),
  ).let {
    telemetryClient.trackEvent(
      "agency-phone-inserted",
      mapOf(
        "agencyId" to agencyId,
        "phoneId" to it.phoneId.toString(),
      ),
    )
    CreateAgencyPhoneNumberResponse(id = it.phoneId)
  }

  private fun phoneTypeOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code))
    ?: throw BadDataException("PhoneUsage with code $code does not exist")

  fun createAgencyAddress(agencyId: String, request: CreateAgencyAddressRequest): CreateAgencyAddressResponse {
    val lookupFailures = mutableMapOf<String, String>()
    val address = agencyLocationAddressRepository.saveAndFlush(
      AgencyLocationAddress(
        agencyLocation = getAgency(agencyId),
        addressType = defaultAddressType(),
        flat = request.flat,
        premise = request.premise,
        street = request.street,
        locality = request.locality,
        postalCode = request.postcode,
        city = cityOf(request.city, lookupFailures),
        county = countyOf(request.county, lookupFailures),
        country = countryOf(request.country, lookupFailures),
        validatedPAF = false,
        noFixedAddress = false,
        primaryAddress = false,
        mailAddress = false,
        comment = null,
        startDate = LocalDate.now(),
        endDate = null,
      ),
    )
    telemetryClient.trackEvent(
      "agency-address-inserted",
      mapOf(
        "agencyId" to agencyId,
        "addressId" to address.addressId.toString(),
      ) + lookupFailures,
    )
    return CreateAgencyAddressResponse(id = address.addressId)
  }

  private fun defaultAddressType(): AddressType = addressTypeRepository.findByIdOrNull(AddressType.pk("BUS"))
    ?: throw BadDataException("AddressType with code BUS does not exist")

  private fun cityOf(description: String?, lookupFailures: MutableMap<String, String>): City? = description?.let {
    cityRepository.findByDomainAndDescriptionIgnoreCase(City.CITY, description) ?: run {
      lookupFailures["cityLookupFailure"] = description
      null
    }
  }

  private fun countyOf(description: String?, lookupFailures: MutableMap<String, String>): County? = description?.let {
    countyRepository.findByDomainAndDescriptionIgnoreCase(County.COUNTY, description) ?: run {
      lookupFailures["countyLookupFailure"] = description
      null
    }
  }

  private fun countryOf(description: String?, lookupFailures: MutableMap<String, String>): Country? = description?.let {
    countryRepository.findByDomainAndDescriptionIgnoreCase(Country.COUNTRY, description) ?: run {
      lookupFailures["countryLookupFailure"] = description
      null
    }
  }

  // used by deletions and updates
  fun updateAgencyEmailAddresses(agencyId: String, request: UpdateAgencyEmailAddressesRequest): AgencyEmailAddressesResponse {
    val agency = getAgency(agencyId)
    val requestedEmailAddresses = request.emailAddresses
    // the common case is a single existing email address being replaced with a single new one, but this will also
    // handle growing or shrinking the list, updating the overlapping entries in place so their ids are preserved
    val existingEmailAddresses = agency.emailAddresses.sortedBy { it.internetAddressId }

    if (existingEmailAddresses.map { it.internetAddress } == requestedEmailAddresses) {
      return existingEmailAddresses.toAgencyEmailAddressesResponse()
    }

    val updatedEmailAddresses = requestedEmailAddresses.mapIndexed { index, emailAddress ->
      existingEmailAddresses.getOrNull(index)?.also { it.internetAddress = emailAddress }
        ?: agencyLocationInternetAddressRepository.save(
          AgencyLocationInternetAddress(
            agencyLocation = agency,
            internetAddress = emailAddress,
            internetAddressClass = EMAIL_INTERNET_ADDRESS_CLASS,
          ),
        )
    }

    if (existingEmailAddresses.size > requestedEmailAddresses.size) {
      agencyLocationInternetAddressRepository.deleteAll(
        existingEmailAddresses.subList(requestedEmailAddresses.size, existingEmailAddresses.size),
      )
    }

    telemetryClient.trackEvent(
      "agency.email.updated",
      mapOf(
        "agencyId" to agencyId,
        "emailAddressIds" to updatedEmailAddresses.joinToString(",") { it.internetAddressId.toString() },
      ),
    )

    return updatedEmailAddresses.toAgencyEmailAddressesResponse()
  }

  // used by deletions and updates
  fun updateAgencyPhoneNumbers(agencyId: String, request: UpdateAgencyPhoneNumbersRequest): AgencyPhoneNumbersResponse {
    val agency = getAgency(agencyId)

    // numbers already held against one of the agency's addresses are left unchanged there, so are excluded
    // from the agency-level list we are refreshing
    request.phoneNumbers.forEach { phoneTypeOf(it.typeCode) }
    val addressPhoneNumbers = agency.addresses.flatMap { it.phones }.map { it.phoneNo }.toSet()
    val requestedPhoneNumbers = request.phoneNumbers.filterNot { it.number in addressPhoneNumbers }

    // the common case is a single existing phone number being replaced with a single new one, but this will also
    // handle growing or shrinking the list, updating the overlapping entries in place so their ids are preserved
    val existingPhoneNumbers = agency.phones.sortedBy { it.phoneId }

    if (existingPhoneNumbers.map { it.toUpdateAgencyPhoneNumber() } == requestedPhoneNumbers) {
      return existingPhoneNumbers.toAgencyPhoneNumbersResponse()
    }

    val updatedPhoneNumbers = requestedPhoneNumbers.mapIndexed { index, phoneNumber ->
      existingPhoneNumbers.getOrNull(index)?.also {
        it.phoneNo = phoneNumber.number
        it.extNo = phoneNumber.extension
        it.phoneType = phoneTypeOf(phoneNumber.typeCode)
      }
        ?: agencyLocationPhoneRepository.save(
          AgencyLocationPhone(
            agencyLocation = agency,
            phoneType = phoneTypeOf(phoneNumber.typeCode),
            phoneNo = phoneNumber.number,
            extNo = phoneNumber.extension,
          ),
        )
    }

    if (existingPhoneNumbers.size > requestedPhoneNumbers.size) {
      agencyLocationPhoneRepository.deleteAll(
        existingPhoneNumbers.subList(requestedPhoneNumbers.size, existingPhoneNumbers.size),
      )
    }

    telemetryClient.trackEvent(
      "agency.phone.updated",
      mapOf(
        "agencyId" to agencyId,
        "phoneIds" to updatedPhoneNumbers.joinToString(",") { it.phoneId.toString() },
      ),
    )

    return updatedPhoneNumbers.toAgencyPhoneNumbersResponse()
  }

  private fun getAgency(agencyId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(agencyId)
    ?: throw NotFoundException("Agency $agencyId does not exist")
}

fun AgencyLocation.toAgencyLocationResponse() = AgencyResponse(
  agencyId = this.id,
  description = this.description,
  longDescription = this.longDescription,
  active = this.active,
  deactivationDate = this.deactivationDate,
  type = this.type.toCodeDescription(),
  updateAllowed = this.updateAllowed,
  contactName = this.contactName,
  disabilityAccessCode = this.disabilityAccessCode,
  area = this.area?.toCodeDescription(),
  subArea = this.subArea?.toCodeDescription(),
  region = this.region?.toCodeDescription(),
  nomsRegion = this.nomsRegion?.toCodeDescription(),
  payrollRegion = this.payrollRegion?.toCodeDescription(),
  cjitCode = this.cjitCode,
  localAuthorities = this.localAuthorities.map { it.authority.toCodeDescription() },
  addresses = this.toAgencyAddresses(),
  phones = this.toPhoneNumbers(),
  emailAddresses = this.toEmailAddresses(),
  district = this.district?.toCodeDescription(),
  courtType = this.courtType?.toCodeDescription(),
)

fun AgencyLocation.toEmailAddresses(): List<AgencyEmailAddress> = emailAddresses.map { email ->
  AgencyEmailAddress(
    id = email.internetAddressId,
    emailAddress = email.internetAddress,
  )
}

fun List<AgencyLocationInternetAddress>.toAgencyEmailAddressesResponse() = AgencyEmailAddressesResponse(
  emailAddresses = this.map { AgencyEmailAddress(id = it.internetAddressId, emailAddress = it.internetAddress) },
)

fun AgencyLocation.toPhoneNumbers(): List<AgencyPhoneNumber> = phones.map { phone ->
  AgencyPhoneNumber(
    id = phone.phoneId,
    number = phone.phoneNo,
    extension = phone.extNo,
    type = phone.phoneType.toCodeDescription(),
  )
}

fun AgencyLocationPhone.toUpdateAgencyPhoneNumber() = UpdateAgencyPhoneNumber(
  number = phoneNo,
  extension = extNo,
  typeCode = phoneType.code,
)

fun List<AgencyLocationPhone>.toAgencyPhoneNumbersResponse() = AgencyPhoneNumbersResponse(
  phoneNumbers = this.map {
    AgencyPhoneNumber(id = it.phoneId, number = it.phoneNo, extension = it.extNo, type = it.phoneType.toCodeDescription())
  },
)

fun AgencyLocation.toAgencyAddresses(): List<AgencyAddress> = addresses.map { address ->
  AgencyAddress(
    id = address.addressId,
    type = address.addressType?.toCodeDescription(),
    flat = address.flat,
    premise = address.premise,
    street = address.street,
    locality = address.locality,
    postcode = address.postalCode,
    city = address.city?.toCodeDescription(),
    county = address.county?.toCodeDescription(),
    country = address.country?.toCodeDescription(),
    validatedPAF = address.validatedPAF,
    primaryAddress = address.primaryAddress,
    noFixedAddress = address.noFixedAddress,
    mailAddress = address.mailAddress,
    comment = address.comment,
    startDate = address.startDate,
    endDate = address.endDate,
    phoneNumbers = address.phones.map { number ->
      AgencyPhoneNumber(
        id = number.phoneId,
        number = number.phoneNo,
        type = number.phoneType.toCodeDescription(),
        extension = number.extNo,
      )
    },
  )
}
