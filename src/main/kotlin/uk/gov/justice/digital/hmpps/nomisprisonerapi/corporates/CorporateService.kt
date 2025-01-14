package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository

@Service
@Transactional
class CorporateService(private val corporateRepository: CorporateRepository) {

  fun getCorporateById(corporateId: Long): CorporateOrganisation =
    corporateRepository.findByIdOrNull(corporateId)?.let {
      CorporateOrganisation(
        id = it.id,
        name = it.corporateName,
        caseload = it.caseload.toCodeDescription(),
        comment = it.commentText,
        programmeNumber = it.feiNumber,
        vatNumber = it.taxNo,
        active = it.active,
        expiryDate = it.expiryDate,
        audit = it.toAudit(),
        types = it.types.map { corporateType ->
          CorporateOrganisationType(
            type = corporateType.type.toCodeDescription(),
            audit = corporateType.toAudit(),
          )
        },
        phoneNumbers = it.phones.map { number ->
          CorporatePhoneNumber(
            id = number.phoneId,
            number = number.phoneNo,
            type = number.phoneType.toCodeDescription(),
            extension = number.extNo,
            audit = number.toAudit(),
          )
        },
        internetAddresses = it.internetAddresses.map { address ->
          CorporateInternetAddress(
            id = address.internetAddressId,
            internetAddress = address.internetAddress,
            type = address.internetAddressClass,
            audit = address.toAudit(),
          )
        },
        addresses = it.addresses.map { address ->
          CorporateAddress(
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
            isServices = address.isServices,
            businessHours = address.businessHours,
            contactPersonName = address.contactPersonName,
            audit = address.toAudit(),
            phoneNumbers = address.phones.map { number ->
              CorporatePhoneNumber(
                id = number.phoneId,
                number = number.phoneNo,
                type = number.phoneType.toCodeDescription(),
                extension = number.extNo,
                audit = number.toAudit(),
              )
            },
          )
        },
      )
    } ?: throw NotFoundException("Corporate not found $corporateId")
}
