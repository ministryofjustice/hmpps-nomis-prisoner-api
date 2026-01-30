package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress.Companion.EMAIL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress.Companion.WEB
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateOrganisationTypePK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporatePhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporatePhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateTypeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Service
@Transactional
class CorporateService(
  private val corporateRepository: CorporateRepository,
  private val caseloadRepository: CaseloadRepository,
  private val corporateAddressRepository: CorporateAddressRepository,
  private val corporatePhoneRepository: CorporatePhoneRepository,
  private val corporateInternetAddressRepository: CorporateInternetAddressRepository,
  private val corporateTypeRepository: CorporateTypeRepository,
  private val addressTypeRepository: ReferenceCodeRepository<AddressType>,
  private val cityRepository: ReferenceCodeRepository<City>,
  private val countyRepository: ReferenceCodeRepository<County>,
  private val countryRepository: ReferenceCodeRepository<Country>,
  private val addressPhoneRepository: AddressPhoneRepository,
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,
  private val corporateOrganisationTypeRepository: ReferenceCodeRepository<uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateOrganisationType>,

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

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
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

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
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

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun deleteCorporate(corporateId: Long) = corporateRepository.deleteById(corporateId)

  fun getCorporateById(corporateId: Long): CorporateOrganisation = corporateRepository.findByIdOrNull(corporateId)?.let {
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

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun createCorporateAddress(corporateId: Long, request: CreateCorporateAddressRequest): CreateCorporateAddressResponse = corporateAddressRepository.saveAndFlush(
    request.let {
      uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress(
        addressType = addressTypeOf(it.typeCode),
        corporate = corporateOf(corporateId),
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
        contactPersonName = it.contactPersonName,
        isServices = it.isServices,
        businessHours = it.businessHours,
      )
    },
  ).let { CreateCorporateAddressResponse(id = it.addressId) }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun updateCorporateAddress(corporateId: Long, addressId: Long, request: UpdateCorporateAddressRequest) {
    addressOf(corporateId = corporateId, addressId = addressId).run {
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
        contactPersonName = it.contactPersonName
        isServices = it.isServices
        businessHours = it.businessHours
      }
    }
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun deleteCorporateAddress(corporateId: Long, addressId: Long) {
    corporateAddressRepository.findByIdOrNull(addressId)?.also {
      if (it.corporate.id != corporateId) throw BadDataException("Address of $addressId does not exist on corporate $corporateId but does on corporate ${it.corporate.id}")
    }
    corporateAddressRepository.deleteById(addressId)
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun createCorporateAddressPhone(corporateId: Long, addressId: Long, request: CreateCorporatePhoneRequest): CreateCorporatePhoneResponse = addressPhoneRepository.saveAndFlush(
    AddressPhone(
      address = addressOf(corporateId = corporateId, addressId = addressId),
      phoneNo = request.number,
      extNo = request.extension,
      phoneType = phoneTypeOf(request.typeCode),
    ),
  ).let { CreateCorporatePhoneResponse(id = it.phoneId) }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun updateCorporateAddressPhone(corporateId: Long, addressId: Long, phoneId: Long, request: UpdateCorporatePhoneRequest) {
    phoneOf(corporateId = corporateId, addressId = addressId, phoneId = phoneId).run {
      request.also {
        phoneNo = it.number
        extNo = it.extension
        phoneType = phoneTypeOf(it.typeCode)
      }
    }
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun deleteCorporateAddressPhone(corporateId: Long, phoneId: Long) {
    addressPhoneRepository.deleteById(phoneId)
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun createCorporatePhone(corporateId: Long, request: CreateCorporatePhoneRequest): CreateCorporatePhoneResponse = corporatePhoneRepository.saveAndFlush(
    CorporatePhone(
      corporate = corporateOf(corporateId),
      phoneNo = request.number,
      extNo = request.extension,
      phoneType = phoneTypeOf(request.typeCode),
    ),
  ).let { CreateCorporatePhoneResponse(id = it.phoneId) }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun updateCorporatePhone(corporateId: Long, phoneId: Long, request: UpdateCorporatePhoneRequest) {
    phoneOf(corporateId = corporateId, phoneId = phoneId).run {
      request.also {
        phoneNo = it.number
        extNo = it.extension
        phoneType = phoneTypeOf(it.typeCode)
      }
    }
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun deleteCorporatePhone(corporateId: Long, phoneId: Long) {
    corporatePhoneRepository.findByIdOrNull(phoneId)?.also {
      if (it.corporate.id != corporateId) throw BadDataException("Phone of $phoneId does not exist on corporate $corporateId but does on corporate ${it.corporate.id}")
    }
    corporatePhoneRepository.deleteById(phoneId)
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun createCorporateEmail(corporateId: Long, request: CreateCorporateEmailRequest): CreateCorporateEmailResponse = corporateInternetAddressRepository.saveAndFlush(
    uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress(
      corporate = corporateOf(corporateId),
      internetAddress = request.email,
      internetAddressClass = EMAIL,
    ),
  ).let { CreateCorporateEmailResponse(id = it.internetAddressId) }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun updateCorporateEmail(corporateId: Long, emailAddressId: Long, request: UpdateCorporateEmailRequest) {
    emailOf(corporateId = corporateId, emailAddressId = emailAddressId).run {
      request.also {
        internetAddress = it.email
      }
    }
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun deleteCorporateEmail(corporateId: Long, emailAddressId: Long) {
    corporateInternetAddressRepository.findByIdOrNull(emailAddressId)?.also {
      if (it.corporate.id != corporateId) throw BadDataException("Internet Address of $emailAddressId does not exist on corporate $corporateId but does on corporate ${it.corporate.id}")
    }
    corporateInternetAddressRepository.deleteById(emailAddressId)
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun createCorporateWebAddress(corporateId: Long, request: CreateCorporateWebAddressRequest): CreateCorporateWebAddressResponse = corporateInternetAddressRepository.saveAndFlush(
    uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress(
      corporate = corporateOf(corporateId),
      internetAddress = request.webAddress,
      internetAddressClass = WEB,
    ),
  ).let { CreateCorporateWebAddressResponse(id = it.internetAddressId) }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun updateCorporateWebAddress(corporateId: Long, webAddressId: Long, request: UpdateCorporateWebAddressRequest) {
    webAddressOf(corporateId = corporateId, webAddressId = webAddressId).run {
      request.also {
        internetAddress = it.webAddress
      }
    }
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun deleteCorporateWebAddress(corporateId: Long, webAddressId: Long) {
    corporateInternetAddressRepository.findByIdOrNull(webAddressId)?.also {
      if (it.corporate.id != corporateId) throw BadDataException("Internet Address of $webAddressId does not exist on corporate $corporateId but does on corporate ${it.corporate.id}")
    }
    corporateInternetAddressRepository.deleteById(webAddressId)
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun createCorporateType(corporateId: Long, request: CreateCorporateTypeRequest) {
    val type = corporateOrganisationTypeOf(request.typeCode)
    corporateTypeRepository.saveAndFlush(
      uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateType(
        id = CorporateOrganisationTypePK(corporateOf(corporateId), typeCode = type.code),
        type = type,
      ),
    )
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun updateCorporateTypes(corporateId: Long, typeCodes: Set<String>) {
    val corporate = corporateOf(corporateId)
    val currentTypeCodes = corporate.types.map { it.type.code }.toSet()
    val typeCodesToAdd = typeCodes subtract currentTypeCodes
    val typeCodesToRemove = currentTypeCodes subtract typeCodes

    corporate.types.removeIf { typeCodesToRemove.contains(it.type.code) }
    typeCodesToAdd.forEach {
      corporate.types.add(
        uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateType(
          id = CorporateOrganisationTypePK(corporate, typeCode = corporateOrganisationTypeOf(it).code),
          type = corporateOrganisationTypeOf(it),
        ),
      )
    }
  }

  @Audit("DPS_SYNCHRONISATION_ORGANISATION")
  fun deleteCorporateType(corporateId: Long, typeCode: String) = corporateTypeRepository.deleteById(CorporateOrganisationTypePK(corporateOf(corporateId), typeCode = typeCode))

  fun caseloadOf(code: String?): Caseload? = code?.let { caseloadRepository.findByIdOrNull(it) ?: throw BadDataException("Caseload $code not found") }
  fun addressTypeOf(code: String?): AddressType? = code?.let { addressTypeRepository.findByIdOrNull(AddressType.pk(code)) ?: throw BadDataException("AddressType with code $code does not exist") }
  fun cityOf(code: String?): City? = code?.let { cityRepository.findByIdOrNull(City.pk(code)) ?: throw BadDataException("City with code $code does not exist") }
  fun countyOf(code: String?): County? = code?.let { countyRepository.findByIdOrNull(County.pk(code)) ?: throw BadDataException("County with code $code does not exist") }
  fun countryOf(code: String?): Country? = code?.let { countryRepository.findByIdOrNull(Country.pk(code)) ?: throw BadDataException("Country with code $code does not exist") }
  fun corporateOf(corporateId: Long): Corporate = corporateRepository.findByIdOrNull(corporateId) ?: throw NotFoundException("Corporate with id=$corporateId does not exist")
  fun addressOf(corporateId: Long, addressId: Long): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress = (corporateAddressRepository.findByIdOrNull(addressId) ?: throw NotFoundException("Address with id=$addressId does not exist")).takeIf { it.corporate == corporateOf(corporateId) } ?: throw NotFoundException("Address with id=$addressId on Corporate with id=$corporateId does not exist")
  fun phoneOf(corporateId: Long, addressId: Long, phoneId: Long) = (addressPhoneRepository.findByIdOrNull(phoneId) ?: throw NotFoundException("Address Phone with id=$phoneId does not exist")).takeIf { it.address == addressOf(corporateId = corporateId, addressId = addressId) } ?: throw NotFoundException("Address Phone with id=$phoneId on Address with id=$addressId on Corporate with id=$corporateId does not exist")
  fun phoneOf(corporateId: Long, phoneId: Long): CorporatePhone = (corporatePhoneRepository.findByIdOrNull(phoneId) ?: throw NotFoundException("Phone with id=$phoneId does not exist")).takeIf { it.corporate == corporateOf(corporateId) } ?: throw NotFoundException("Phone with id=$phoneId on Corporate with id=$corporateId does not exist")
  fun phoneTypeOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code)) ?: throw BadDataException("PhoneUsage with code $code does not exist")
  fun corporateOrganisationTypeOf(code: String): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateOrganisationType = corporateOrganisationTypeRepository.findByIdOrNull(uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateOrganisationType.pk(code)) ?: throw BadDataException("CorporateOrganisationType with code $code does not exist")
  fun emailOf(corporateId: Long, emailAddressId: Long): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress = (corporateInternetAddressRepository.findByIdOrNull(emailAddressId) ?: throw NotFoundException("Email with id=$emailAddressId does not exist")).takeIf { it.corporate == corporateOf(corporateId) } ?: throw NotFoundException("Email with id=$emailAddressId on Corporate with id=$corporateId does not exist")
  fun webAddressOf(corporateId: Long, webAddressId: Long): uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress = (corporateInternetAddressRepository.findByIdOrNull(webAddressId) ?: throw NotFoundException("Web address with id=$webAddressId does not exist")).takeIf { it.corporate == corporateOf(corporateId) } ?: throw NotFoundException("Web address with id=$webAddressId on Corporate with id=$corporateId does not exist")
}

data class CorporateFilter(
  val fromDate: LocalDate?,
  val toDate: LocalDate?,
)
