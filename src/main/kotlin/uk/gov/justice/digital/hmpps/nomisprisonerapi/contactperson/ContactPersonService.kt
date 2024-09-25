package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts.NomisAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository

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
      audit = NomisAudit(
        createDatetime = it.createDatetime,
        createUsername = it.createUsername,
        createDisplayName = it.createStaffUserAccount?.staff.asDisplayName(),
        modifyDatetime = it.modifyDatetime,
        modifyUserId = it.modifyUserId,
        modifyDisplayName = it.modifyStaffUserAccount?.staff.asDisplayName(),
        auditUserId = it.auditUserId,
        auditTimestamp = it.auditTimestamp,
        auditModuleName = it.auditModuleName,
        auditAdditionalInfo = it.auditAdditionalInfo,
        auditClientIpAddress = it.auditClientIpAddress,
        auditClientUserId = it.auditClientUserId,
        auditClientWorkstationName = it.auditClientWorkstationName,
      ),
      phoneNumbers = it.phones.map { number ->
        PhoneNumber(
          phoneId = number.phoneId,
          number = number.phoneNo,
          type = number.phoneType.toCodeDescription(),
          extension = number.extNo,
        )
      },
      emailAddresses = it.internetAddresses.map { address ->
        EmailAddress(
          emailAddressId = address.internetAddressId,
          email = address.internetAddress,
        )
      },
      employments = it.employments.map { employment ->
        Employment(
          sequence = employment.id.sequence,
          active = employment.active,
          corporate = employment.employerCorporate?.let { corporate ->
            Corporate(
              id = corporate.id,
              name = corporate.corporateName,
            )
          },
        )
      },
      addresses = it.addresses.map { address ->
        Address(
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
          phoneNumbers = address.phones.map { number ->
            PhoneNumber(
              phoneId = number.phoneId,
              number = number.phoneNo,
              type = number.phoneType.toCodeDescription(),
              extension = number.extNo,
            )
          },
        )
      },
    )
  } ?: throw NotFoundException("Person not found $personId")
}

private fun Staff?.asDisplayName(): String? = this?.let { "${it.firstName} ${it.lastName}" }
