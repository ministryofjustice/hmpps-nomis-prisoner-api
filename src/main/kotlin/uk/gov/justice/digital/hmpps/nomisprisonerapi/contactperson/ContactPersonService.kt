package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.usernamePreferringGeneralAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IdentifierType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Language
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MaritalStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPersonRestrict
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifierPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RestrictionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Title
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitorRestriction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderContactPersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPersonRestrictRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonIdentifierRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitorRestrictionRepository
import java.time.LocalDate

@Transactional
@Service
class ContactPersonService(
  private val bookingRepository: OffenderBookingRepository,
  private val personRepository: PersonRepository,
  private val contactRepository: OffenderContactPersonRepository,
  private val personAddressRepository: PersonAddressRepository,
  private val personInternetAddressRepository: PersonInternetAddressRepository,
  private val personPhoneRepository: PersonPhoneRepository,
  private val addressPhoneRepository: AddressPhoneRepository,
  private val personIdentifierRepository: PersonIdentifierRepository,
  private val personRestrictionRepository: VisitorRestrictionRepository,
  private val personContactRestrictionRepository: OffenderPersonRestrictRepository,
  private val genderRepository: ReferenceCodeRepository<Gender>,
  private val titleRepository: ReferenceCodeRepository<Title>,
  private val languageRepository: ReferenceCodeRepository<Language>,
  private val maritalStatusRepository: ReferenceCodeRepository<MaritalStatus>,
  private val contactTypeRepository: ReferenceCodeRepository<ContactType>,
  private val relationshipTypeRepository: ReferenceCodeRepository<RelationshipType>,
  private val addressTypeRepository: ReferenceCodeRepository<AddressType>,
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,
  private val cityRepository: ReferenceCodeRepository<City>,
  private val countyRepository: ReferenceCodeRepository<County>,
  private val countryRepository: ReferenceCodeRepository<Country>,
  private val identifierRepository: ReferenceCodeRepository<IdentifierType>,
  private val restrictionTypeRepository: ReferenceCodeRepository<RestrictionType>,
  private val staffUserAccountRepository: StaffUserAccountRepository,
) {
  fun getPerson(personId: Long): ContactPerson = personRepository.findByIdOrNull(personId)?.let {
    ContactPerson(
      personId = it.id,
      firstName = it.firstName,
      lastName = it.lastName,
      middleName = it.middleName,
      dateOfBirth = it.birthDate,
      gender = it.sex?.toCodeDescription(),
      title = it.title?.toCodeDescription(),
      language = it.language?.toCodeDescription(),
      interpreterRequired = it.interpreterRequired,
      domesticStatus = it.domesticStatus?.toCodeDescription(),
      deceasedDate = it.deceasedDate,
      isStaff = it.isStaff,
      isRemitter = it.isRemitter,
      audit = it.toAudit(),
      phoneNumbers = it.phones.map { number ->
        PersonPhoneNumber(
          phoneId = number.phoneId,
          number = number.phoneNo,
          type = number.phoneType.toCodeDescription(),
          extension = number.extNo,
          audit = number.toAudit(),
        )
      },
      emailAddresses = it.internetAddresses.map { address ->
        PersonEmailAddress(
          emailAddressId = address.internetAddressId,
          email = address.internetAddress,
          audit = address.toAudit(),
        )
      },
      employments = it.employments.map { employment ->
        PersonEmployment(
          sequence = employment.id.sequence,
          active = employment.active,
          audit = employment.toAudit(),
          corporate = employment.employerCorporate.let { corporate ->
            PersonEmploymentCorporate(
              id = corporate.id,
              name = corporate.corporateName,
            )
          },
        )
      },
      identifiers = it.identifiers.map { personIdentifier ->
        PersonIdentifier(
          sequence = personIdentifier.id.sequence,
          type = personIdentifier.identifierType.toCodeDescription(),
          identifier = personIdentifier.identifier,
          issuedAuthority = personIdentifier.issuedAuthority,
          audit = personIdentifier.toAudit(),
        )
      },
      addresses = it.addresses.map { address ->
        PersonAddress(
          addressId = address.addressId,
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
          audit = address.toAudit(),
          phoneNumbers = address.phones.map { number ->
            PersonPhoneNumber(
              phoneId = number.phoneId,
              number = number.phoneNo,
              type = number.phoneType.toCodeDescription(),
              extension = number.extNo,
              audit = number.toAudit(),
            )
          },
        )
      },
      restrictions = it.restrictions.map { restriction ->
        ContactRestriction(
          id = restriction.id,
          type = restriction.restrictionType.toCodeDescription(),
          comment = restriction.comment,
          effectiveDate = restriction.effectiveDate,
          expiryDate = restriction.expiryDate,
          enteredStaff = restriction.enteredStaff.toContactRestrictionEnteredStaff(),
          audit = restriction.toAudit(),
        )
      },
      contacts = it.contacts.map { contact ->
        PersonContact(
          id = contact.id,
          contactType = contact.contactType.toCodeDescription(),
          relationshipType = contact.relationshipType.toCodeDescription(),
          active = contact.active,
          approvedVisitor = contact.approvedVisitor ?: false,
          emergencyContact = contact.emergencyContact,
          nextOfKin = contact.nextOfKin,
          expiryDate = contact.expiryDate,
          comment = contact.comment,
          audit = contact.toAudit(),
          prisoner = contact.offenderBooking.let { booking ->
            ContactForPrisoner(
              bookingId = booking.bookingId,
              bookingSequence = booking.bookingSequence.toLong(),
              offenderNo = booking.offender.nomsId,
              lastName = booking.offender.lastName,
              firstName = booking.offender.firstName,
            )
          },
          restrictions = contact.restrictions.map { restriction ->
            ContactRestriction(
              id = restriction.id,
              type = restriction.restrictionType.toCodeDescription(),
              comment = restriction.comment,
              effectiveDate = restriction.effectiveDate,
              expiryDate = restriction.expiryDate,
              enteredStaff = restriction.enteredStaff.toContactRestrictionEnteredStaff(),
              audit = restriction.toAudit(),
            )
          },
        )
      },
    )
  } ?: throw NotFoundException("Person not found $personId")

  fun findPersonIdsByFilter(
    pageRequest: Pageable,
    personFilter: PersonFilter,
  ): Page<PersonIdResponse> = if (personFilter.toDate == null && personFilter.fromDate == null) {
    personRepository.findAllPersonIds(
      pageRequest,
    )
  } else {
    personRepository.findAllPersonIds(
      fromDate = personFilter.fromDate?.atStartOfDay(),
      toDate = personFilter.toDate?.atStartOfDay(),
      pageRequest,
    )
  }.map { PersonIdResponse(personId = it.personId) }

  fun createPerson(request: CreatePersonRequest): CreatePersonResponse {
    assertDoesNotExist(request)

    request.let {
      Person(
        id = it.personId ?: 0,
        lastName = it.lastName.uppercase(),
        firstName = it.firstName.uppercase(),
        middleName = it.middleName?.uppercase(),
        birthDate = it.dateOfBirth,
        sex = genderOf(it.genderCode),
        title = titleOf(it.titleCode),
        language = languageOf(it.languageCode),
        interpreterRequired = it.interpreterRequired,
        domesticStatus = martialStatusOf(it.domesticStatusCode),
        isStaff = it.isStaff,
      )
    }
      .let { personRepository.save(it) }
      .let { return CreatePersonResponse(it.id) }
  }

  fun updatePerson(personId: Long, request: UpdatePersonRequest) {
    personOf(personId).run {
      request.also {
        lastName = it.lastName.uppercase()
        firstName = it.firstName.uppercase()
        middleName = it.middleName?.uppercase()
        birthDate = it.dateOfBirth
        sex = genderOf(it.genderCode)
        title = titleOf(it.titleCode)
        language = languageOf(it.languageCode)
        interpreterRequired = it.interpreterRequired
        domesticStatus = martialStatusOf(it.domesticStatusCode)
        isStaff = it.isStaff
        deceasedDate = it.deceasedDate
      }
    }
  }

  fun deletePerson(personId: Long) = personRepository.deleteById(personId)

  fun createPersonContact(personId: Long, request: CreatePersonContactRequest): CreatePersonContactResponse {
    val booking = bookingRepository.findLatestByOffenderNomsId(request.offenderNo) ?: throw BadDataException("Prisoner with nomisId=${request.offenderNo} does not exist")
    val person = personOf(personId)
    if (booking.contacts.any { it.contactType.code == request.contactTypeCode && it.relationshipType.code == request.relationshipTypeCode && person == it.person }) {
      throw ConflictException("Prisoner ${request.offenderNo} with booking ${booking.bookingId} already is a contact with person $personId for contactType ${request.contactTypeCode} and relationshipType ${request.relationshipTypeCode} ")
    }
    val contact = OffenderContactPerson(
      offenderBooking = booking,
      person = person,
      contactType = contactType(request.contactTypeCode),
      relationshipType = relationshipType(request.relationshipTypeCode),
      rootOffender = null,
      active = request.active,
      approvedVisitor = request.approvedVisitor,
      emergencyContact = request.emergencyContact,
      nextOfKin = request.nextOfKin,
      comment = request.comment,
      expiryDate = request.expiryDate,
    )

    return CreatePersonContactResponse(contactRepository.save(contact).id)
  }

  fun updatePersonContact(personId: Long, contactId: Long, request: UpdatePersonContactRequest) {
    val contact = contactOf(personId = personId, contactId = contactId)
    // check if another contact with the same person already has the relationship type
    // e.g. you can't be the BROTHER to teh same prisoner twice
    if (contact.offenderBooking.contacts.filter { it.id != contactId }.any { it.contactType.code == request.contactTypeCode && it.relationshipType.code == request.relationshipTypeCode && personId == it.person!!.id }) {
      throw ConflictException("Prisoner with booking  ${contact.offenderBooking.offender.nomsId} with booking ${contact.offenderBooking.bookingId} already is a contact with person $personId for contactType ${request.contactTypeCode} and relationshipType ${request.relationshipTypeCode} ")
    }

    contact.run {
      request.also {
        contactType = contactType(it.contactTypeCode)
        relationshipType = relationshipType(it.relationshipTypeCode)
        active = it.active
        approvedVisitor = it.approvedVisitor
        emergencyContact = it.emergencyContact
        nextOfKin = it.nextOfKin
        comment = it.comment
        expiryDate = it.expiryDate
      }
    }
  }

  fun createPersonAddress(personId: Long, request: CreatePersonAddressRequest): CreatePersonAddressResponse = personAddressRepository.saveAndFlush(
    request.let {
      uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress(
        addressType = addressTypeOf(it.typeCode),
        person = personOf(personId),
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
  ).let { CreatePersonAddressResponse(personAddressId = it.addressId) }

  fun createPersonEmail(personId: Long, request: CreatePersonEmailRequest): CreatePersonEmailResponse = personInternetAddressRepository.saveAndFlush(
    PersonInternetAddress(
      person = personOf(personId),
      emailAddress = request.email,
    ),
  ).let { CreatePersonEmailResponse(emailAddressId = it.internetAddressId) }

  fun createPersonPhone(personId: Long, request: CreatePersonPhoneRequest): CreatePersonPhoneResponse = personPhoneRepository.saveAndFlush(
    PersonPhone(
      person = personOf(personId),
      phoneNo = request.number,
      extNo = request.extension,
      phoneType = phoneTypeOf(request.typeCode),
    ),
  ).let { CreatePersonPhoneResponse(phoneId = it.phoneId) }

  fun createPersonAddressPhone(personId: Long, addressId: Long, request: CreatePersonPhoneRequest): CreatePersonPhoneResponse = addressPhoneRepository.saveAndFlush(
    AddressPhone(
      address = addressOf(personId = personId, addressId = addressId),
      phoneNo = request.number,
      extNo = request.extension,
      phoneType = phoneTypeOf(request.typeCode),
    ),
  ).let { CreatePersonPhoneResponse(phoneId = it.phoneId) }

  fun createPersonIdentifier(personId: Long, request: CreatePersonIdentifierRequest): CreatePersonIdentifierResponse = personOf(personId).let { person ->
    personIdentifierRepository.saveAndFlush(
      uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifier(
        id = PersonIdentifierPK(person = person, sequence = personIdentifierRepository.getNextSequence(person)),
        identifier = request.identifier,
        identifierType = identifierTypeOf(request.typeCode),
        issuedAuthority = request.issuedAuthority,
      ),
    ).let { CreatePersonIdentifierResponse(sequence = it.id.sequence) }
  }

  fun createPersonContactRestriction(
    personId: Long,
    contactId: Long,
    request: CreateContactPersonRestrictionRequest,
  ): CreateContactPersonRestrictionResponse = personContactRestrictionRepository.saveAndFlush(
    OffenderPersonRestrict(
      restrictionType = restrictionTypeOf(request.typeCode),
      contactPerson = contactOf(personId = personId, contactId = contactId),
      comment = request.comment,
      effectiveDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      enteredStaff = staffOf(username = request.enteredStaffUsername),
    ),
  ).let { CreateContactPersonRestrictionResponse(id = it.id) }

  fun createPersonRestriction(
    personId: Long,
    request: CreateContactPersonRestrictionRequest,
  ): CreateContactPersonRestrictionResponse = personRestrictionRepository.saveAndFlush(
    VisitorRestriction(
      person = personOf(personId),
      restrictionType = restrictionTypeOf(request.typeCode),
      comment = request.comment,
      effectiveDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      enteredStaff = staffOf(username = request.enteredStaffUsername),
    ),
  ).let { CreateContactPersonRestrictionResponse(id = it.id) }

  fun updatePersonRestriction(
    personId: Long,
    personRestrictionId: Long,
    request: UpdateContactPersonRestrictionRequest,
  ) {
    val restriction = personOf(personId).restrictions.find { it.id == personRestrictionId }
      ?: throw NotFoundException("Restriction with id: $personRestrictionId not found on person $personId")
    with(request) {
      restriction.restrictionType = restrictionTypeOf(typeCode)
      restriction.comment = comment
      restriction.effectiveDate = effectiveDate
      restriction.expiryDate = expiryDate
      restriction.enteredStaff = staffOf(username = enteredStaffUsername)
    }
  }

  fun updatePersonContactRestriction(
    personId: Long,
    contactId: Long,
    contactRestrictionId: Long,
    request: UpdateContactPersonRestrictionRequest,
  ) {
    val restriction = contactOf(personId, contactId).restrictions.find { it.id == contactRestrictionId }
      ?: throw NotFoundException("Restriction with id: $contactRestrictionId not found on contact $contactId orn person $personId")
    with(request) {
      restriction.restrictionType = restrictionTypeOf(typeCode)
      restriction.comment = comment
      restriction.effectiveDate = effectiveDate
      restriction.expiryDate = expiryDate
      restriction.enteredStaff = staffOf(username = enteredStaffUsername)
    }
  }

  fun assertDoesNotExist(request: CreatePersonRequest) {
    request.personId?.takeIf { it != 0L }
      ?.run {
        if (personRepository.existsById(this)) {
          throw ConflictException("Person with id=$this already exists")
        }
      }
  }
  fun contactType(code: String): ContactType = contactTypeRepository.findByIdOrNull(ContactType.pk(code)) ?: throw BadDataException("contactType with code $code does not exist")
  fun relationshipType(code: String): RelationshipType = relationshipTypeRepository.findByIdOrNull(RelationshipType.pk(code)) ?: throw BadDataException("relationshipType with code $code does not exist")
  fun genderOf(code: String?): Gender? = code?.let { genderRepository.findByIdOrNull(Gender.pk(it)) }
  fun titleOf(code: String?): Title? = code?.let { titleRepository.findByIdOrNull(Title.pk(it)) }
  fun languageOf(code: String?): Language? = code?.let { languageRepository.findByIdOrNull(Language.pk(it)) }
  fun martialStatusOf(code: String?): MaritalStatus? = code?.let { maritalStatusRepository.findByIdOrNull(MaritalStatus.pk(it)) }
  fun addressTypeOf(code: String?): AddressType? = code?.let { addressTypeRepository.findByIdOrNull(AddressType.pk(code)) ?: throw BadDataException("AddressType with code $code does not exist") }
  fun cityOf(code: String?): City? = code?.let { cityRepository.findByIdOrNull(City.pk(code)) ?: throw BadDataException("City with code $code does not exist") }
  fun countyOf(code: String?): County? = code?.let { countyRepository.findByIdOrNull(County.pk(code)) ?: throw BadDataException("County with code $code does not exist") }
  fun countryOf(code: String?): Country? = code?.let { countryRepository.findByIdOrNull(Country.pk(code)) ?: throw BadDataException("Country with code $code does not exist") }
  fun personOf(personId: Long): Person = personRepository.findByIdOrNull(personId) ?: throw NotFoundException("Person with id=$personId does not exist")
  fun staffOf(username: String): Staff = staffUserAccountRepository.findByUsername(username)?.staff ?: throw BadDataException("Staff with username=$username does not exist")
  fun addressOf(personId: Long, addressId: Long): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress = (personAddressRepository.findByIdOrNull(addressId) ?: throw NotFoundException("Person with id=$personId does not exist")).takeIf { it.person == personOf(personId) } ?: throw NotFoundException("Address with id=$addressId on Person with id=$personId does not exist")
  fun phoneTypeOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code)) ?: throw BadDataException("PhoneUsage with code $code does not exist")
  fun identifierTypeOf(code: String): IdentifierType = identifierRepository.findByIdOrNull(IdentifierType.pk(code)) ?: throw BadDataException("IdentifierType with code $code does not exist")
  fun restrictionTypeOf(code: String): RestrictionType = restrictionTypeRepository.findByIdOrNull(RestrictionType.pk(code)) ?: throw BadDataException("RestrictionType with code $code does not exist")
  fun contactOf(personId: Long, contactId: Long): OffenderContactPerson = (contactRepository.findByIdOrNull(contactId) ?: throw NotFoundException("Contact with id=$contactId does not exist")).takeIf { it.person == personOf(personId) } ?: throw NotFoundException("Contact with id=$contactId on Person with id=$personId does not exist")
}

data class PersonFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

private fun Staff.toContactRestrictionEnteredStaff() = ContactRestrictionEnteredStaff(staffId = this.id, username = this.usernamePreferringGeneralAccount())
