package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import java.time.LocalDate

@Service
@Transactional
class CorporateService(
  private val corporateRepository: CorporateRepository,
  private val caseloadRepository: CaseloadRepository,
) {
  fun findCorporateIdsByFilter(
    pageRequest: Pageable,
    filter: CorporateFilter,
  ): Page<CorporateOrganisationIdResponse> = if (filter.toDate == null && filter.fromDate == null) {
    corporateRepository.findAllCorporateIds(
      pageRequest,
    )
  } else {
    corporateRepository.findAllCorporateIds(
      fromDate = filter.fromDate?.atStartOfDay(),
      toDate = filter.toDate?.atStartOfDay(),
      pageRequest,
    )
  }.map { CorporateOrganisationIdResponse(corporateId = it.corporateId) }

  fun createCorporate(request: CreateCorporateOrganisationRequest) {
    corporateRepository.save(
      request.let {
        Corporate(
          id = it.id,
          corporateName = it.name,
          caseload = caseloadOf(it.caseloadId),
          commentText = it.comment,
          suspended = false,
          feiNumber = it.programmeNumber,
          active = it.active,
          expiryDate = it.expiryDate,
          taxNo = it.vatNumber,
        )
      },
    )
  }

  fun updateCorporate(corporateId: Long, request: UpdateCorporateOrganisationRequest) {
    request.also {
      corporateRepository.findByIdOrNull(corporateId)?.run {
        corporateName = it.name
        caseload = caseloadOf(it.caseloadId)
        commentText = it.comment
        suspended = false
        feiNumber = it.programmeNumber
        active = it.active
        expiryDate = it.expiryDate
        taxNo = it.vatNumber
      } ?: throw NotFoundException("Corporate $corporateId not found")
    }
  }

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
    } ?: throw NotFoundException("Corporate $corporateId not found")

  fun caseloadOf(code: String?): Caseload? = code?.let { caseloadRepository.findByIdOrNull(it) ?: throw BadDataException("Caseload $code not found") }
}

data class CorporateFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)
