package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import java.time.LocalDate

@Transactional
@Service
class ContactPersonService(private val personRepository: PersonRepository) {
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
      keepBiometrics = it.keepBiometrics,
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
          corporate = employment.employerCorporate?.let { corporate ->
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
          enteredStaff = ContactRestrictionEnteredStaff(staffId = restriction.enteredStaff.id),
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
              enteredStaff = ContactRestrictionEnteredStaff(staffId = restriction.enteredStaff.id),
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
  ): Page<PersonIdResponse> =
    if (personFilter.toDate == null && personFilter.fromDate == null) {
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
}

data class PersonFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)
