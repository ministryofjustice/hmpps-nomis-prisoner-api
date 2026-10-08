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

  private fun cityOf(description: String?, lookupFailures: MutableMap<String, String>, key: String = "cityLookupFailure"): City? = description?.let {
    cityRepository.findByDomainAndDescriptionIgnoreCase(City.CITY, description) ?: run {
      lookupFailures[key] = description
      null
    }
  }

  private fun countyOf(description: String?, lookupFailures: MutableMap<String, String>, key: String = "countyLookupFailure"): County? = description?.let {
    countyRepository.findByDomainAndDescriptionIgnoreCase(County.COUNTY, description) ?: run {
      lookupFailures[key] = description
      null
    }
  }

  private fun countryOf(description: String?, lookupFailures: MutableMap<String, String>, key: String = "countryLookupFailure"): Country? = description?.let {
    countryRepository.findByDomainAndDescriptionIgnoreCase(Country.COUNTRY, description) ?: run {
      lookupFailures[key] = description
      null
    }
  }

  // used by deletions and updates
  fun updateAgencyAddresses(agencyId: String, request: UpdateAgencyAddressesRequest): AgencyAddressesResponse {
    val agency = getAgency(agencyId)
    val requestedAddresses = request.addresses
    val existingAddresses = agency.addresses

    val lookupFailures = mutableMapOf<String, String>()
    val indexKeySuffix = if (requestedAddresses.size > 1) "[%d]" else ""

    // the common case is a single existing address being replaced with a single new one - there is no id in the
    // request to correlate the two, but since there is only one of each they can only mean each other
    val updatedAddresses = if (
      existingAddresses.size == 1 &&
      requestedAddresses.size == 1 &&
      !existingAddresses[0].postalCode.isNullOrBlank() &&
      !requestedAddresses[0].postcode.isNullOrBlank()
    ) {
      listOf(updateAddress(existingAddresses[0], requestedAddresses[0], lookupFailures, indexKeySuffix.format(0)))
    } else {
      // with more than one address there is still no id to correlate by, so existing and requested addresses are
      // instead matched up explicitly by postcode. Existing addresses are only available to be matched once each;
      // where more than one existing or requested address shares the same postcode they are matched in the order
      // the existing addresses were created (by id). A null (or blank) postcode is never treated as a match, even
      // against another null postcode, since it carries no identifying information - matching it would risk
      // silently reassigning the identity (and id) of an unrelated address. So: existing addresses with a null
      // postcode that are not otherwise referenced are removed, and requested addresses with a null postcode
      // always result in a new address being created
      val unmatchedExistingByPostcode = existingAddresses
        .filterNot { it.postalCode.isNullOrBlank() }
        .sortedBy { it.addressId }
        .groupByTo(mutableMapOf()) { it.postalCode!! }
        .mapValuesTo(mutableMapOf()) { (_, addresses) -> ArrayDeque(addresses) }

      val matchedExistingIds = mutableSetOf<Long>()

      val result = requestedAddresses.mapIndexed { index, address ->
        val suffix = indexKeySuffix.format(index)
        val matchingExisting = address.postcode
          ?.takeIf { it.isNotBlank() }
          ?.let { unmatchedExistingByPostcode[it]?.removeFirstOrNull() }

        matchingExisting
          ?.also { matchedExistingIds.add(it.addressId) }
          ?.let { updateAddress(it, address, lookupFailures, suffix) }
          ?: createAddress(agency, address, lookupFailures, suffix)
      }

      val addressesToRemove = existingAddresses.filterNot { it.addressId in matchedExistingIds }
      if (addressesToRemove.isNotEmpty()) {
        agencyLocationAddressRepository.deleteAll(addressesToRemove)
      }

      result
    }

    telemetryClient.trackEvent(
      "agency.address.updated",
      mapOf(
        "agencyId" to agencyId,
        "addressIds" to updatedAddresses.joinToString(",") { it.addressId.toString() },
      ) + lookupFailures,
    )

    return updatedAddresses.toAgencyAddressesResponse()
  }

  private fun updateAddress(existing: AgencyLocationAddress, address: UpdateAgencyAddress, lookupFailures: MutableMap<String, String>, telemetryKeySuffix: String): AgencyLocationAddress = existing.also {
    it.flat = address.flat
    it.premise = address.premise
    it.street = address.street
    it.locality = address.locality
    it.postalCode = address.postcode
    it.city = cityOf(address.city, lookupFailures, "cityLookupFailure$telemetryKeySuffix")
    it.county = countyOf(address.county, lookupFailures, "countyLookupFailure$telemetryKeySuffix")
    it.country = countryOf(address.country, lookupFailures, "countryLookupFailure$telemetryKeySuffix")
  }

  private fun createAddress(agency: AgencyLocation, address: UpdateAgencyAddress, lookupFailures: MutableMap<String, String>, telemetryKeySuffix: String): AgencyLocationAddress = agencyLocationAddressRepository.save(
    AgencyLocationAddress(
      agencyLocation = agency,
      addressType = defaultAddressType(),
      flat = address.flat,
      premise = address.premise,
      street = address.street,
      locality = address.locality,
      postalCode = address.postcode,
      startDate = LocalDate.now(),
      city = cityOf(address.city, lookupFailures, "cityLookupFailure$telemetryKeySuffix"),
      county = countyOf(address.county, lookupFailures, "countyLookupFailure$telemetryKeySuffix"),
      country = countryOf(address.country, lookupFailures, "countryLookupFailure$telemetryKeySuffix"),
    ),
  )

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

fun AgencyLocation.toAgencyAddresses(): List<AgencyAddress> = addresses.map { it.toAgencyAddress() }

fun AgencyLocationAddress.toAgencyAddress(): AgencyAddress = AgencyAddress(
  id = addressId,
  type = addressType?.toCodeDescription(),
  flat = flat,
  premise = premise,
  street = street,
  locality = locality,
  postcode = postalCode,
  city = city?.toCodeDescription(),
  county = county?.toCodeDescription(),
  country = country?.toCodeDescription(),
  validatedPAF = validatedPAF,
  primaryAddress = primaryAddress,
  noFixedAddress = noFixedAddress,
  mailAddress = mailAddress,
  comment = comment,
  startDate = startDate,
  endDate = endDate,
  phoneNumbers = phones.map { number ->
    AgencyPhoneNumber(
      id = number.phoneId,
      number = number.phoneNo,
      type = number.phoneType.toCodeDescription(),
      extension = number.extNo,
    )
  },
)

fun List<AgencyLocationAddress>.toAgencyAddressesResponse() = AgencyAddressesResponse(
  addresses = this.map { it.toAgencyAddress() },
)
