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
      addresses = it.addresses.map { address ->
        Address(
          addressId = address.addressId,
          type = address.addressType?.toCodeDescription(),
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
