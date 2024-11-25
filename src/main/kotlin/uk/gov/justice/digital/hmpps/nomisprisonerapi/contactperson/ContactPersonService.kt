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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Language
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MaritalStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Title
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderContactPersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Transactional
@Service
class ContactPersonService(
  private val bookingRepository: OffenderBookingRepository,
  private val personRepository: PersonRepository,
  private val contactRepository: OffenderContactPersonRepository,
  private val genderRepository: ReferenceCodeRepository<Gender>,
  private val titleRepository: ReferenceCodeRepository<Title>,
  private val languageRepository: ReferenceCodeRepository<Language>,
  private val maritalStatusRepository: ReferenceCodeRepository<MaritalStatus>,
  private val contactTypeRepository: ReferenceCodeRepository<ContactType>,
  private val relationshipTypeRepository: ReferenceCodeRepository<RelationshipType>,
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

  fun createPersonContact(personId: Long, request: CreatePersonContactRequest): CreatePersonContactResponse {
    val booking = bookingRepository.findLatestByOffenderNomsId(request.offenderNo) ?: throw BadDataException("Prisoner with nomisId=${request.offenderNo} does not exist")
    val person = personRepository.findByIdOrNull(personId) ?: throw NotFoundException("Person with id=$personId does not exist")
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
}

data class PersonFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)

private fun Staff.toContactRestrictionEnteredStaff() = ContactRestrictionEnteredStaff(staffId = this.id, username = this.usernamePreferringGeneralAccount())
