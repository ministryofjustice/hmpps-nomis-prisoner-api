package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifierPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBeliefRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIdentifierRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@Transactional
@Service
class CorePersonService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderBeliefRepository: OffenderBeliefRepository,
  private val offenderIdentifierRepository: OffenderIdentifierRepository,
  private val offenderInternetAddressRepository: OffenderInternetAddressRepository,
  private val offenderPhoneRepository: OffenderPhoneRepository,
  private val offenderAddressRepository: OffenderAddressRepository,
  private val addressPhoneRepository: AddressPhoneRepository,
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,
  private val addressTypeRepository: ReferenceCodeRepository<AddressType>,
  private val cityRepository: ReferenceCodeRepository<City>,
  private val countyRepository: ReferenceCodeRepository<County>,
  private val countryRepository: ReferenceCodeRepository<Country>,
) {
  fun getOffender(prisonNumber: String): CorePerson {
    val latestBooking = offenderBookingRepository.findLatestByOffenderNomsId(prisonNumber)
    val (currentAlias, rootOffender) = currentAliasAndRootOffender(prisonNumber)
    val allOffenders = offenderRepository.findByNomsId(prisonNumber).sortedBy { it.id }

    return CorePerson(
      prisonNumber = prisonNumber,
      inOutStatus = latestBooking?.inOutStatus ?: "OUT",
      activeFlag = latestBooking?.active ?: false,
      offenders = allOffenders.map {
        it.toCoreOffender(currentAlias.id)
      },
      addresses = getAddresses(rootOffender),
      phoneNumbers = getPhoneNumbers(rootOffender),
      emailAddresses = getEmailAddresses(rootOffender),
      beliefs = offenderBeliefRepository.findBeliefsByRootOffenderId(rootOffender.id)
        .map { it.toBelief() },
    )
  }

  fun getOffenderReligions(prisonNumber: String): List<OffenderBelief> = offenderBeliefRepository.findBeliefsByPrisonNumber(prisonNumber)
    .map { it.toBelief() }

  fun getOffenderEmail(offenderId: Long, emailAddressId: Long): OffenderEmailAddress = emailOf(
    offenderId = offenderId,
    emailAddressId = emailAddressId,
  ).toOffenderEmailAddress()

  fun createOffenderEmail(offenderId: Long, request: CreateOffenderEmailRequest): CreateOffenderEmailResponse = offenderInternetAddressRepository.saveAndFlush(
    OffenderInternetAddress(
      offender = offenderOf(offenderId),
      emailAddress = request.email,
    ),
  ).let { CreateOffenderEmailResponse(emailAddressId = it.internetAddressId) }

  fun updateOffenderEmail(offenderId: Long, emailAddressId: Long, request: UpdateOffenderEmailRequest) {
    emailOf(offenderId = offenderId, emailAddressId = emailAddressId).run {
      request.also {
        internetAddress = it.email
      }
    }
  }

  fun deleteOffenderEmail(offenderId: Long, emailAddressId: Long) {
    offenderInternetAddressRepository.findByIdOrNull(emailAddressId)?.also {
      if (it.offender.id != offenderId) throw BadDataException("Internet Address of $emailAddressId does not exist on offender $offenderId but does on offender ${it.offender.id}")
    }
    offenderInternetAddressRepository.deleteById(emailAddressId)
  }

  fun getOffenderPhone(offenderId: Long, phoneId: Long): OffenderPhoneNumber = phoneOf(
    offenderId = offenderId,
    phoneId = phoneId,
  ).toOffenderPhoneNumber()

  fun createOffenderPhone(offenderId: Long, request: CreateOffenderPhoneRequest): CreateOffenderPhoneResponse = offenderPhoneRepository.saveAndFlush(
    OffenderPhone(
      offender = offenderOf(offenderId),
      phoneNo = request.number,
      extNo = request.extension,
      phoneType = phoneTypeOf(request.typeCode),
    ),
  ).let { CreateOffenderPhoneResponse(phoneId = it.phoneId) }

  fun updateOffenderPhone(offenderId: Long, phoneId: Long, request: UpdateOffenderPhoneRequest) {
    phoneOf(offenderId = offenderId, phoneId = phoneId).run {
      request.also {
        phoneNo = it.number
        extNo = it.extension
        phoneType = phoneTypeOf(it.typeCode)
      }
    }
  }

  fun deleteOffenderPhone(offenderId: Long, phoneId: Long) {
    offenderPhoneRepository.findByIdOrNull(phoneId)?.also {
      if (it.offender.id != offenderId) throw BadDataException("Phone of $phoneId does not exist on offender $offenderId but does on offender ${it.offender.id}")
    }
    offenderPhoneRepository.deleteById(phoneId)
  }

  fun getOffenderAddress(offenderId: Long, addressId: Long): OffenderAddress = offenderAddressOf(
    offenderId = offenderId,
    addressId = addressId,
  ).toOffenderAddress()

  fun createOffenderAddress(offenderId: Long, request: CreateOffenderAddressRequest): CreateOffenderAddressResponse = offenderAddressRepository.saveAndFlush(
    request.let {
      uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress(
        offender = offenderOf(offenderId),
        addressType = addressTypeOf(it.typeCode),
        premise = it.premise,
        street = it.street,
        locality = it.locality,
        flat = it.flat,
        postalCode = it.postcode,
        city = cityOf(it.cityCode),
        county = countyOf(it.countyCode),
        country = countryOf(it.countryCode),
        validatedPAF = false,
        noFixedAddress = it.noFixedAddress,
        primaryAddress = it.primaryAddress,
        mailAddress = it.mailAddress,
        comment = it.comment,
        startDate = it.startDate,
        endDate = it.endDate,
      )
    },
  ).let { CreateOffenderAddressResponse(addressId = it.addressId) }

  fun updateOffenderAddress(offenderId: Long, addressId: Long, request: UpdateOffenderAddressRequest) {
    offenderAddressOf(offenderId = offenderId, addressId = addressId).run {
      request.also {
        addressType = addressTypeOf(it.typeCode)
        premise = it.premise
        street = it.street
        locality = it.locality
        flat = it.flat
        postalCode = it.postcode
        city = cityOf(it.cityCode)
        county = countyOf(it.countyCode)
        country = countryOf(it.countryCode)
        noFixedAddress = it.noFixedAddress
        primaryAddress = it.primaryAddress
        mailAddress = it.mailAddress
        comment = it.comment
        startDate = it.startDate
        endDate = it.endDate
        it.validatedPAF?.also { validated ->
          validatedPAF = validated
        }
      }
    }
  }

  fun deleteOffenderAddress(offenderId: Long, addressId: Long) {
    offenderAddressRepository.findByIdOrNull(addressId)?.also {
      if (it.offender.id != offenderId) throw BadDataException("Address of $addressId does not exist on offender $offenderId but does on offender ${it.offender.id}")
    }
    offenderAddressRepository.deleteById(addressId)
  }

  fun getOffenderAddressPhone(offenderId: Long, addressId: Long, phoneId: Long): OffenderPhoneNumber = addressPhoneOf(
    offenderId = offenderId,
    addressId = addressId,
    phoneId = phoneId,
  ).toOffenderPhoneNumber()

  fun createOffenderAddressPhone(offenderId: Long, addressId: Long, request: CreateOffenderPhoneRequest): CreateOffenderPhoneResponse = addressPhoneRepository.saveAndFlush(
    uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone(
      address = offenderAddressOf(offenderId = offenderId, addressId = addressId),
      phoneNo = request.number,
      extNo = request.extension,
      phoneType = phoneTypeOf(request.typeCode),
    ),
  ).let { CreateOffenderPhoneResponse(phoneId = it.phoneId) }

  fun updateOffenderAddressPhone(offenderId: Long, addressId: Long, phoneId: Long, request: UpdateOffenderPhoneRequest) {
    addressPhoneOf(offenderId = offenderId, addressId = addressId, phoneId = phoneId).run {
      request.also {
        phoneNo = it.number
        extNo = it.extension
        phoneType = phoneTypeOf(it.typeCode)
      }
    }
  }

  fun deleteOffenderAddressPhone(offenderId: Long, addressId: Long, phoneId: Long) {
    addressPhoneRepository.findByIdOrNull(phoneId)?.also {
      if (it.address.addressId != addressId) throw BadDataException("Phone of $phoneId does not exist on address $addressId but does on address ${it.address.addressId}")
    }
    offenderAddressRepository.findByIdOrNull(addressId)?.also {
      if (it.offender.id != offenderId) throw BadDataException("Address of $addressId does not exist on offender $offenderId but does on offender ${it.offender.id}")
    }

    addressPhoneRepository.deleteById(phoneId)
  }

  fun updateOffenderAfterMerge(prisonNumber: String, request: CorePersonMergeRequest) {
    log.info("Updating offender {} after merge", prisonNumber)
    val offender =
      offenderRepository.findRootByNomsId(prisonNumber) ?: throw NotFoundException("Offender not found $prisonNumber")
    val religionsToUpdate =
      offenderBeliefRepository.findAllById(request.religions.map { it.beliefId }).associateBy { it.beliefId }
    for ((beliefId, endDate) in request.religions) {
      val toUpdate = religionsToUpdate[beliefId] ?: throw NotFoundException("Religion not found $beliefId")
      if (toUpdate.rootOffenderId != offender.id) {
        toUpdate.rootOffenderId = offender.id
      }
      toUpdate.endDate = endDate
    }
  }

  fun getIdentifier(offenderId: Long, sequenceNumber: Int): Identifier = offenderIdentifierRepository.findById(OffenderIdentifierPK(offenderOf(offenderId), sequenceNumber.toLong()))
    .orElseThrow { NotFoundException("Identifier not found for offender $offenderId and sequence $sequenceNumber") }
    .toIdentifier()

  fun getAlias(offenderId: Long): CoreOffender {
    val offender = offenderOf(offenderId)
    val prisonNumber = offender.nomsId
    val (currentAlias, _) = currentAliasAndRootOffender(prisonNumber)
    return offender.toCoreOffender(currentAliasId = currentAlias.id, includeIdentifiers = false)
  }

  fun getOffenderAliasesAndIdentifiers(prisonNumber: String): List<CoreOffender> {
    val (currentAlias, _) = currentAliasAndRootOffender(prisonNumber)
    return offenderRepository.findByNomsId(prisonNumber).sortedBy { it.id }.map {
      it.toCoreOffender(currentAliasId = currentAlias.id, includeIdentifiers = true)
    }
  }

  fun getAddressesAndContacts(prisonNumber: String): CorePersonAddressContact = rootOffender(prisonNumber).let { rootOffender ->
    CorePersonAddressContact(
      addresses = getAddresses(rootOffender),
      emailAddresses = getEmailAddresses(rootOffender),
      phoneNumbers = getPhoneNumbers(rootOffender),
    )
  }

  private fun getAddresses(rootOffender: Offender): List<OffenderAddress> = rootOffender.addresses.map { it.toOffenderAddress() }

  private fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress.toOffenderAddress(): OffenderAddress = OffenderAddress(
    addressId = addressId,
    flat = flat,
    premise = premise,
    street = street,
    locality = locality,
    postcode = postalCode,
    city = city?.toCodeDescription(),
    county = county?.toCodeDescription(),
    country = country?.toCodeDescription(),
    primaryAddress = primaryAddress,
    noFixedAddress = noFixedAddress,
    mailAddress = mailAddress,
    comment = comment,
    startDate = startDate,
    endDate = endDate,
    phoneNumbers = phones.map { number -> number.toOffenderPhoneNumber() },
    usages = usages.filter { u -> u.addressUsage != null }.map { u ->
      OffenderAddressUsage(
        addressId = addressId,
        usage = u.addressUsage!!.toCodeDescription(),
        active = u.active,
        createdDateTime = u.createDatetime,
        createdByUsername = u.createUsername,
        lastUpdatedDateTime = u.modifyDatetime,
        lastUpdatedByUsername = u.modifyUserId,
      )
    },
    createdDateTime = createDatetime,
    createdByUsername = createUsername,
    lastUpdatedDateTime = modifyDatetime,
    lastUpdatedByUsername = modifyUserId,
  )

  private fun getPhoneNumbers(rootOffender: Offender): List<OffenderPhoneNumber> = rootOffender.phones.map { number ->
    number.toOffenderPhoneNumber()
  }

  private fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Phone.toOffenderPhoneNumber() = OffenderPhoneNumber(
    phoneId = phoneId,
    number = phoneNo,
    type = phoneType.toCodeDescription(),
    extension = extNo,
    createdDateTime = createDatetime,
    createdByUsername = createUsername,
    lastUpdatedDateTime = modifyDatetime,
    lastUpdatedByUsername = modifyUserId,
  )

  private fun getEmailAddresses(rootOffender: Offender): List<OffenderEmailAddress> = rootOffender.internetAddresses.map { address ->
    address.toOffenderEmailAddress()
  }

  private fun OffenderInternetAddress.toOffenderEmailAddress() = OffenderEmailAddress(
    emailAddressId = internetAddressId,
    email = internetAddress,
    createdDateTime = createDatetime,
    createdByUsername = createUsername,
    lastUpdatedDateTime = modifyDatetime,
    lastUpdatedByUsername = modifyUserId,
  )

  private fun currentAliasAndRootOffender(prisonNumber: String): CurrentAliasAndRoot {
    val rootOffender = rootOffender(prisonNumber)
    val currentAlias =
      offenderBookingRepository.findLatestByOffenderNomsId(prisonNumber)?.offender ?: rootOffender
    return CurrentAliasAndRoot(currentAlias, rootOffender)
  }

  private fun rootOffender(prisonNumber: String): Offender = offenderRepository.findRootByNomsId(prisonNumber)
    ?: throw NotFoundException("Offender not found $prisonNumber")

  private data class CurrentAliasAndRoot(val currentAlias: Offender, val rootOffender: Offender)

  private fun offenderOf(offenderId: Long) = offenderRepository.findById(offenderId).orElseThrow { NotFoundException("Offender not found $offenderId") }

  private fun emailOf(offenderId: Long, emailAddressId: Long): OffenderInternetAddress = (offenderInternetAddressRepository.findByIdOrNull(emailAddressId) ?: throw NotFoundException("Email with id=$emailAddressId does not exist")).takeIf { it.offender.id == offenderId } ?: throw NotFoundException("Email with id=$emailAddressId on Offender with id=$offenderId does not exist")

  private fun phoneOf(offenderId: Long, phoneId: Long): OffenderPhone = (offenderPhoneRepository.findByIdOrNull(phoneId) ?: throw NotFoundException("Phone with id=$phoneId does not exist")).takeIf { it.offender.id == offenderId } ?: throw NotFoundException("Phone with id=$phoneId on Offender with id=$offenderId does not exist")

  private fun phoneTypeOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code)) ?: throw BadDataException("PhoneUsage with code $code does not exist")

  private fun addressTypeOf(code: String?): AddressType? = code?.let { addressTypeRepository.findByIdOrNull(AddressType.pk(code)) ?: throw BadDataException("AddressType with code $code does not exist") }

  private fun cityOf(code: String?): City? = code?.let { cityRepository.findByIdOrNull(City.pk(code)) ?: throw BadDataException("City with code $code does not exist") }

  private fun countyOf(code: String?): County? = code?.let { countyRepository.findByIdOrNull(County.pk(code)) ?: throw BadDataException("County with code $code does not exist") }

  private fun countryOf(code: String?): Country? = code?.let { countryRepository.findByIdOrNull(Country.pk(code)) ?: throw BadDataException("Country with code $code does not exist") }

  private fun offenderAddressOf(offenderId: Long, addressId: Long): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress = (offenderAddressRepository.findByIdOrNull(addressId) ?: throw NotFoundException("Address with id=$addressId does not exist")).takeIf { it.offender.id == offenderId } ?: throw NotFoundException("Address with id=$addressId on Offender with id=$offenderId does not exist")

  private fun addressPhoneOf(offenderId: Long, addressId: Long, phoneId: Long): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone = (addressPhoneRepository.findByIdOrNull(phoneId) ?: throw NotFoundException("Address Phone with id=$phoneId does not exist")).takeIf { it.address.addressId == offenderAddressOf(offenderId = offenderId, addressId = addressId).addressId } ?: throw NotFoundException("Address Phone with id=$phoneId on Address with id=$addressId on Offender with id=$offenderId does not exist")

  private fun OffenderIdentifier.toIdentifier(): Identifier = Identifier(
    offenderId = id.offender.id,
    sequence = id.sequence,
    type = identifierType.toCodeDescription(),
    identifier = identifier,
    issuedAuthority = issuedAuthority,
    issuedDate = issuedDate,
    verified = verified ?: false,
  )

  private fun Offender.toCoreOffender(currentAliasId: Long, includeIdentifiers: Boolean = true): CoreOffender = CoreOffender(
    offenderId = id,
    title = title?.toCodeDescription(),
    firstName = firstName,
    middleName1 = middleName,
    middleName2 = middleName2,
    lastName = lastName,
    dateOfBirth = birthDate,
    birthPlace = birthPlace,
    birthCountry = birthCountry?.toCodeDescription(),
    ethnicity = ethnicity?.toCodeDescription(),
    sex = gender.toCodeDescription(),
    nameType = nameType?.toCodeDescription(),
    createDate = createDate,
    workingName = id == currentAliasId,
    identifiers = if (includeIdentifiers) {
      identifiers.map { id ->
        Identifier(
          offenderId = id.id.offender.id,
          sequence = id.id.sequence,
          type = id.identifierType.toCodeDescription(),
          identifier = id.identifier,
          issuedAuthority = id.issuedAuthority,
          issuedDate = id.issuedDate,
          verified = id.verified ?: false,
        )
      }
    } else {
      emptyList()
    },
  )

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

private fun uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief.toBelief(): OffenderBelief = OffenderBelief(
  beliefId = beliefId,
  belief = beliefCode.toCodeDescription(),
  startDate = startDate,
  endDate = endDate,
  changeReason = changeReason,
  comments = comments,
  audit = toAudit(),
)
