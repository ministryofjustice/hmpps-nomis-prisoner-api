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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IdentifierType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Language
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MaritalStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPersonRestrict
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderRestrictions
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmploymentPK
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderContactPersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPersonRestrictRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRestrictionsRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonEmploymentRepository
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
  private val personEmploymentRepository: PersonEmploymentRepository,
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
  private val corporateRepository: CorporateRepository,
  private val offenderRestrictionsRepository: OffenderRestrictionsRepository,
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

  fun getPrisonerWithContacts(offenderNo: String, activeOnly: Boolean, latestBookingOnly: Boolean): PrisonerWithContacts = bookingRepository.findAllByOffenderNomsId(offenderNo)
    .filter { latestBookingOnly == false || it.bookingSequence == 1 }
    .flatMap { booking ->
      booking.contacts
        // only interested in person contacts - not other prisoners
        .filter { it.person != null }
        .filter { activeOnly == false || it.active }
        .map { contact ->
          PrisonerContact(
            id = contact.id,
            bookingId = booking.bookingId,
            bookingSequence = booking.bookingSequence.toLong(),
            contactType = contact.contactType.toCodeDescription(),
            relationshipType = contact.relationshipType.toCodeDescription(),
            active = contact.active,
            approvedVisitor = contact.approvedVisitor == true,
            emergencyContact = contact.emergencyContact,
            nextOfKin = contact.nextOfKin,
            expiryDate = contact.expiryDate,
            comment = contact.comment,
            person = ContactForPerson(
              personId = contact.person!!.id,
              lastName = contact.person!!.lastName,
              firstName = contact.person!!.firstName,
            ),
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
            audit = contact.toAudit(),
          )
        }
    }.let { PrisonerWithContacts(contacts = it) }

  fun getPrisonerWithRestrictions(offenderNo: String, latestBookingOnly: Boolean): PrisonerWithRestrictions = bookingRepository.findAllByOffenderNomsId(offenderNo)
    .filter { !latestBookingOnly || it.bookingSequence == 1 }
    .flatMap { booking ->
      booking.restrictions
        .map { restriction ->
          PrisonerRestriction(
            id = restriction.id,
            bookingId = booking.bookingId,
            bookingSequence = booking.bookingSequence.toLong(),
            offenderNo = booking.offender.nomsId,
            type = restriction.restrictionType.toCodeDescription(),
            comment = restriction.comment,
            effectiveDate = restriction.effectiveDate,
            expiryDate = restriction.expiryDate,
            enteredStaff = restriction.enteredStaff.toContactRestrictionEnteredStaff(),
            authorisedStaff = restriction.authorisedStaff.toContactRestrictionEnteredStaff(),
            audit = restriction.toAudit(),
          )
        }
    }.let { PrisonerWithRestrictions(restrictions = it) }

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

  fun findPersonIdsFromId(personId: Long, pageSize: Int): PersonIdsWithLast = personRepository.findAllIdsFromId(personId = personId, pageSize = pageSize).let {
    PersonIdsWithLast(lastPersonId = it.lastOrNull() ?: 0, personIds = it)
  }

  fun findOffenderRestrictionIdsByFilter(
    pageRequest: Pageable,
    restrictionFilter: RestrictionFilter,
  ): Page<PrisonerRestrictionIdResponse> = if (restrictionFilter.toDate == null && restrictionFilter.fromDate == null) {
    offenderRestrictionsRepository.findAllIds(
      pageRequest,
    )
  } else {
    offenderRestrictionsRepository.findAllIds(
      fromDate = restrictionFilter.fromDate?.atStartOfDay(),
      toDate = restrictionFilter.toDate?.atStartOfDay(),
      pageRequest,
    )
  }.map { PrisonerRestrictionIdResponse(restrictionId = it.restrictionId) }

  fun findOffenderRestrictionIdsFromId(restrictionId: Long, pageSize: Int): RestrictionIdsWithLast = offenderRestrictionsRepository.findAllIdsFromId(restrictionId = restrictionId, pageSize = pageSize).let {
    RestrictionIdsWithLast(lastRestrictionId = it.lastOrNull() ?: 0, restrictionIds = it)
  }

  fun getPrisonerRestriction(restrictionId: Long): PrisonerRestriction = offenderRestrictionsRepository.findByIdOrNull(restrictionId)?.let { restriction ->
    val booking = restriction.offenderBooking
    PrisonerRestriction(
      id = restriction.id,
      bookingId = booking.bookingId,
      bookingSequence = booking.bookingSequence.toLong(),
      offenderNo = booking.offender.nomsId,
      type = restriction.restrictionType.toCodeDescription(),
      comment = restriction.comment,
      effectiveDate = restriction.effectiveDate,
      expiryDate = restriction.expiryDate,
      enteredStaff = restriction.enteredStaff.toContactRestrictionEnteredStaff(),
      authorisedStaff = restriction.authorisedStaff.let {
        ContactRestrictionEnteredStaff(
          staffId = it.id,
          username = it.usernamePreferringGeneralAccount(),
        )
      },
      audit = restriction.toAudit(),
    )
  } ?: throw NotFoundException("Restriction not found $restrictionId")

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

  fun deletePersonContact(personId: Long, contactId: Long) {
    contactRepository.findByIdOrNull(contactId)?.also {
      if (it.person?.id != personId) throw BadDataException("Contact of $contactId does not exist on person $personId but does on person ${it.person?.id}")
    }
    contactRepository.deleteById(contactId)
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

  fun updatePersonAddress(personId: Long, addressId: Long, request: UpdatePersonAddressRequest) {
    addressOf(personId = personId, addressId = addressId).run {
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
        it.validatedPAF ?.also { validated ->
          validatedPAF = validated
        }
      }
    }
  }

  fun deletePersonAddress(personId: Long, addressId: Long) {
    personAddressRepository.findByIdOrNull(addressId)?.also {
      if (it.person.id != personId) throw BadDataException("Address of $addressId does not exist on person $personId but does on person ${it.person.id}")
    }
    personAddressRepository.deleteById(addressId)
  }

  fun createPersonEmail(personId: Long, request: CreatePersonEmailRequest): CreatePersonEmailResponse = personInternetAddressRepository.saveAndFlush(
    PersonInternetAddress(
      person = personOf(personId),
      emailAddress = request.email,
    ),
  ).let { CreatePersonEmailResponse(emailAddressId = it.internetAddressId) }

  fun updatePersonEmail(personId: Long, emailAddressId: Long, request: UpdatePersonEmailRequest) {
    emailOf(personId = personId, emailAddressId = emailAddressId).run {
      request.also {
        internetAddress = it.email
      }
    }
  }

  fun deletePersonEmail(personId: Long, emailAddressId: Long) {
    personInternetAddressRepository.findByIdOrNull(emailAddressId)?.also {
      if (it.person.id != personId) throw BadDataException("Internet Address of $emailAddressId does not exist on person $personId but does on person ${it.person.id}")
    }
    personInternetAddressRepository.deleteById(emailAddressId)
  }

  fun createPersonPhone(personId: Long, request: CreatePersonPhoneRequest): CreatePersonPhoneResponse = personPhoneRepository.saveAndFlush(
    PersonPhone(
      person = personOf(personId),
      phoneNo = request.number,
      extNo = request.extension,
      phoneType = phoneTypeOf(request.typeCode),
    ),
  ).let { CreatePersonPhoneResponse(phoneId = it.phoneId) }

  fun updatePersonPhone(personId: Long, phoneId: Long, request: UpdatePersonPhoneRequest) {
    phoneOf(personId = personId, phoneId = phoneId).run {
      request.also {
        phoneNo = it.number
        extNo = it.extension
        phoneType = phoneTypeOf(it.typeCode)
      }
    }
  }

  fun deletePersonPhone(personId: Long, phoneId: Long) {
    personPhoneRepository.findByIdOrNull(phoneId)?.also {
      if (it.person.id != personId) throw BadDataException("Phone of $phoneId does not exist on person $personId but does on person ${it.person.id}")
    }
    personPhoneRepository.deleteById(phoneId)
  }

  fun updatePersonAddressPhone(personId: Long, addressId: Long, phoneId: Long, request: UpdatePersonPhoneRequest) {
    phoneOf(personId = personId, addressId = addressId, phoneId = phoneId).run {
      request.also {
        phoneNo = it.number
        extNo = it.extension
        phoneType = phoneTypeOf(it.typeCode)
      }
    }
  }

  fun createPersonAddressPhone(personId: Long, addressId: Long, request: CreatePersonPhoneRequest): CreatePersonPhoneResponse = addressPhoneRepository.saveAndFlush(
    AddressPhone(
      address = addressOf(personId = personId, addressId = addressId),
      phoneNo = request.number,
      extNo = request.extension,
      phoneType = phoneTypeOf(request.typeCode),
    ),
  ).let { CreatePersonPhoneResponse(phoneId = it.phoneId) }

  fun deletePersonAddressPhone(personId: Long, addressId: Long, phoneId: Long) {
    addressPhoneRepository.findByIdOrNull(phoneId)?.also {
      if (it.address.addressId != addressId) throw BadDataException("Phone of $phoneId does not exist on address $addressId but does on address ${it.address.addressId}")
    }
    personAddressRepository.findByIdOrNull(addressId)?.also {
      if (it.person.id != personId) throw BadDataException("Address of $addressId does not exist on person $personId but does on person ${it.person.id}")
    }

    addressPhoneRepository.deleteById(phoneId)
  }

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

  fun updatePersonIdentifier(personId: Long, sequence: Long, request: UpdatePersonIdentifierRequest) {
    identifierOf(personId, sequence = sequence).run {
      request.also {
        identifier = it.identifier
        identifierType = identifierTypeOf(it.typeCode)
        issuedAuthority = it.issuedAuthority
      }
    }
  }

  fun deletePersonIdentifier(personId: Long, sequence: Long) {
    personIdentifierRepository.deleteById(PersonIdentifierPK(person = personOf(personId), sequence = sequence))
  }

  fun createPersonEmployment(personId: Long, request: CreatePersonEmploymentRequest): CreatePersonEmploymentResponse = personOf(personId).let { person ->
    personEmploymentRepository.saveAndFlush(
      uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmployment(

        id = PersonEmploymentPK(person = person, sequence = personEmploymentRepository.getNextSequence(person)),
        active = request.active,
        employerCorporate = corporateOf(request.corporateId),
      ),
    ).let { CreatePersonEmploymentResponse(sequence = it.id.sequence) }
  }

  fun updatePersonEmployment(personId: Long, sequence: Long, request: UpdatePersonEmploymentRequest) {
    employmentOf(personId, sequence = sequence).run {
      request.also {
        active = it.active
        employerCorporate = corporateOf(it.corporateId)
      }
    }
  }

  fun deletePersonEmployment(personId: Long, sequence: Long) {
    personEmploymentRepository.deleteById(PersonEmploymentPK(person = personOf(personId), sequence = sequence))
  }

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

  fun deletePersonRestriction(personId: Long, personRestrictionId: Long) {
    personRestrictionRepository.findByIdOrNull(personRestrictionId)?.also {
      if (it.person.id != personId) throw BadDataException("Restriction of $personRestrictionId does not exist on person $personId but does on person ${it.person.id}")
    }
    personRestrictionRepository.deleteById(personRestrictionId)
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

  fun deletePersonContactRestriction(personId: Long, contactId: Long, contactRestrictionId: Long) {
    personContactRestrictionRepository.findByIdOrNull(contactRestrictionId)?.also {
      if (it.contactPerson.id != contactId) throw BadDataException("Contact Restriction of $contactRestrictionId does not exist on contact $contactId but does on contact ${it.contactPerson.id}")
    }
    contactRepository.findByIdOrNull(contactId)?.also {
      if (it.person?.id != personId) throw BadDataException("Contact of $contactId does not exist on person $personId but does on person ${it.person?.id}")
    }

    personContactRestrictionRepository.deleteById(contactRestrictionId)
  }

  fun createPrisonerRestriction(
    offenderNo: String,
    request: CreatePrisonerRestrictionRequest,
  ): CreatePrisonerRestrictionResponse = offenderRestrictionsRepository.saveAndFlush(
    OffenderRestrictions(
      offenderBooking = bookingRepository.findLatestByOffenderNomsId(offenderNo) ?: throw NotFoundException("Prisoner with nomisId=$offenderNo does not exist"),
      restrictionType = restrictionTypeOf(request.typeCode),
      comment = request.comment,
      effectiveDate = request.effectiveDate,
      expiryDate = request.expiryDate,
      enteredStaff = staffOf(username = request.enteredStaffUsername),
      authorisedStaff = staffOf(username = request.authorisedStaffUsername),
    ),
  ).let { CreatePrisonerRestrictionResponse(id = it.id) }

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
  fun addressOf(personId: Long, addressId: Long): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress = (personAddressRepository.findByIdOrNull(addressId) ?: throw NotFoundException("Address with id=$addressId does not exist")).takeIf { it.person == personOf(personId) } ?: throw NotFoundException("Address with id=$addressId on Person with id=$personId does not exist")
  fun phoneOf(personId: Long, phoneId: Long): PersonPhone = (personPhoneRepository.findByIdOrNull(phoneId) ?: throw NotFoundException("Phone with id=$phoneId does not exist")).takeIf { it.person == personOf(personId) } ?: throw NotFoundException("Phone with id=$phoneId on Person with id=$personId does not exist")
  fun emailOf(personId: Long, emailAddressId: Long): PersonInternetAddress = (personInternetAddressRepository.findByIdOrNull(emailAddressId) ?: throw NotFoundException("EMail with id=$emailAddressId does not exist")).takeIf { it.person == personOf(personId) } ?: throw NotFoundException("Email with id=$emailAddressId on Person with id=$personId does not exist")
  fun phoneOf(personId: Long, addressId: Long, phoneId: Long) = (addressPhoneRepository.findByIdOrNull(phoneId) ?: throw NotFoundException("Address Phone with id=$phoneId does not exist")).takeIf { it.address == addressOf(personId = personId, addressId = addressId) } ?: throw NotFoundException("Address Phone with id=$phoneId on Address with id=$addressId on Person with id=$personId does not exist")
  fun identifierOf(personId: Long, sequence: Long) = personIdentifierRepository.findByIdOrNull(PersonIdentifierPK(personOf(personId), sequence)) ?: throw NotFoundException("Identifier with sequence=$sequence does not exist on person $personId")
  fun employmentOf(personId: Long, sequence: Long) = personEmploymentRepository.findByIdOrNull(PersonEmploymentPK(personOf(personId), sequence)) ?: throw NotFoundException("Employment with sequence=$sequence does not exist on person $personId")
  fun corporateOf(corporateId: Long): Corporate = corporateRepository.findByIdOrNull(corporateId) ?: throw BadDataException("Corporate with id $corporateId does not exist")
  fun phoneTypeOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code)) ?: throw BadDataException("PhoneUsage with code $code does not exist")
  fun identifierTypeOf(code: String): IdentifierType = identifierRepository.findByIdOrNull(IdentifierType.pk(code)) ?: throw BadDataException("IdentifierType with code $code does not exist")
  fun restrictionTypeOf(code: String): RestrictionType = restrictionTypeRepository.findByIdOrNull(RestrictionType.pk(code)) ?: throw BadDataException("RestrictionType with code $code does not exist")
  fun contactOf(personId: Long, contactId: Long): OffenderContactPerson = (contactRepository.findByIdOrNull(contactId) ?: throw NotFoundException("Contact with id=$contactId does not exist")).takeIf { it.person == personOf(personId) } ?: throw NotFoundException("Contact with id=$contactId on Person with id=$personId does not exist")

  fun getContact(contactId: Long): PersonContact {
    val contact = contactRepository.findByIdOrNull(contactId) ?: throw NotFoundException("Contact with id=$contactId does not exist")
    return PersonContact(
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
  }
}

data class PersonFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

data class RestrictionFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

private fun Staff.toContactRestrictionEnteredStaff() = ContactRestrictionEnteredStaff(staffId = this.id, username = this.usernamePreferringGeneralAccount())
