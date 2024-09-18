package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts.NomisAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository

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
    )
  } ?: throw NotFoundException("Person not found $personId")
}

private fun Staff?.asDisplayName(): String? = this?.let { "${it.firstName} ${it.lastName}" }
