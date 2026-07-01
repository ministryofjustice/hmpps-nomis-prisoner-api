package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Agency
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAuthority
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAuthorityId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Area
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LocalAuthorityType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayrollRegionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Prison
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class AgencyLocationDslMarker

@AgencyLocationDslMarker
interface AgencyLocationDsl {
  companion object {
    const val SHEFFIELD = "25343"
    const val BRENT = "00AE"
    const val BROMLEY = "00AF"
  }

  fun address(
    type: String? = "BUS",
    premise: String? = "2",
    street: String? = "Gloucester Terrace",
    locality: String? = "Stanningley Road",
    flat: String? = null,
    postcode: String? = "LS12 2TJ",
    city: String? = "29059",
    county: String? = "W.YORKSHIRE",
    country: String? = "ENG",
    validatedPAF: Boolean = false,
    noFixedAddress: Boolean? = null,
    primaryAddress: Boolean = true,
    mailAddress: Boolean = false,
    comment: String? = null,
    startDate: String? = null,
    endDate: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: AgencyLocationAddressDsl.() -> Unit = {},
  ): AgencyLocationAddress

  fun phone(
    phoneType: String = "BUS",
    phoneNo: String,
    extNo: String? = null,
    dsl: AgencyLocationPhoneDsl.() -> Unit = {},
  ): AgencyLocationPhone

  fun email(
    address: String,
    dsl: AgencyLocationInternetAddressDsl.() -> Unit = {},
  ): AgencyLocationInternetAddress

  fun localAuthority(code: String): AgencyLocationAuthority
}

@Component
class AgencyLocationBuilderRepository(
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyRepository: AgencyRepository,
  private val prisonRepository: PrisonRepository,
  private val agencyTypeRepository: ReferenceCodeRepository<AgencyLocationType>,
  private val courtTypeRepository: ReferenceCodeRepository<CourtType>,
  private val payrollRegionTypeRepository: ReferenceCodeRepository<PayrollRegionType>,
  private val localAuthorityTypeRepository: ReferenceCodeRepository<LocalAuthorityType>,
) {
  fun save(agencyLocation: AgencyLocation): AgencyLocation = agencyLocationRepository.saveAndFlush(agencyLocation)
  fun saveAgency(agency: Agency): Agency = agencyRepository.saveAndFlush(agency)
  fun savePrison(prison: Prison): Prison = prisonRepository.saveAndFlush(prison)
  fun agencyTypeOf(code: String): AgencyLocationType = agencyTypeRepository.findByIdOrNull(AgencyLocationType.pk(code))!!
  fun courtTypeOf(code: String?): CourtType? = code?.let { courtTypeRepository.findByIdOrNull(CourtType.pk(code)) }
  fun payrollRegionOf(code: String?): PayrollRegionType? = code?.let { payrollRegionTypeRepository.findByIdOrNull(PayrollRegionType.pk(code)) }
  fun localAuthorityTypeOf(code: String): LocalAuthorityType = localAuthorityTypeRepository.findByIdOrNull(LocalAuthorityType.pk(code))!!
}

@Component
class AgencyLocationBuilderFactory(
  private val repository: AgencyLocationBuilderRepository,
  private val agencyAddressBuilderFactory: AgencyLocationAddressBuilderFactory,
  private val agencyPhoneBuilderFactory: AgencyLocationPhoneBuilderFactory,
  private val agencyLocationInternetAddressBuilderFactory: AgencyLocationInternetAddressBuilderFactory,
) {
  fun builder(): AgencyLocationBuilder = AgencyLocationBuilder(
    repository,
    agencyAddressBuilderFactory,
    agencyPhoneBuilderFactory,
    agencyLocationInternetAddressBuilderFactory,
  )
}

class AgencyLocationBuilder(
  private val repository: AgencyLocationBuilderRepository,
  private val agencyAddressBuilderFactory: AgencyLocationAddressBuilderFactory,
  private val agencyPhoneBuilderFactory: AgencyLocationPhoneBuilderFactory,
  private val agencyLocationInternetAddressBuilderFactory: AgencyLocationInternetAddressBuilderFactory,
) : AgencyLocationDsl {
  private lateinit var agencyLocation: AgencyLocation

  fun build(
    id: String,
    description: String,
    longDescription: String?,
    type: String,
    active: Boolean,
    district: Area?,
    deactivationDate: LocalDate?,
    updateAllowed: Boolean,
    contactName: String?,
    courtTypeCode: String?,
    disabilityAccessCode: String?,
    subArea: Area?,
    area: Area?,
    region: Area?,
    nomsRegion: Area?,
    cjitCode: String?,
    payrollRegionCode: String?,
  ): AgencyLocation = AgencyLocation(
    id = id,
    description = description,
    longDescription = longDescription,
    type = repository.agencyTypeOf(type),
    active = active,
    district = district,
    deactivationDate = deactivationDate,
    updateAllowed = updateAllowed,
    contactName = contactName,
    courtType = repository.courtTypeOf(courtTypeCode),
    disabilityAccessCode = disabilityAccessCode,
    subArea = subArea,
    area = area,
    region = region,
    nomsRegion = nomsRegion,
    cjitCode = cjitCode,
    payrollRegion = repository.payrollRegionOf(payrollRegionCode),
  )
    .let { repository.save(it) }
    .also { agencyLocation = it }

  fun buildAgency(
    id: String,
    description: String,
    longDescription: String?,
    type: String,
    active: Boolean,
    district: Area?,
    deactivationDate: LocalDate?,
    updateAllowed: Boolean,
    contactName: String?,
    courtTypeCode: String?,
    disabilityAccessCode: String?,
    subArea: Area?,
    area: Area?,
    region: Area?,
    nomsRegion: Area?,
    cjitCode: String?,
    payrollRegionCode: String?,
  ): Agency = Agency(
    id = id,
    description = description,
    longDescription = longDescription,
    type = repository.agencyTypeOf(type),
    active = active,
    district = district,
    deactivationDate = deactivationDate,
    updateAllowed = updateAllowed,
    contactName = contactName,
    courtType = repository.courtTypeOf(courtTypeCode),
    disabilityAccessCode = disabilityAccessCode,
    subArea = subArea,
    area = area,
    region = region,
    nomsRegion = nomsRegion,
    cjitCode = cjitCode,
    payrollRegion = repository.payrollRegionOf(payrollRegionCode),
  )
    .let { repository.saveAgency(it) }
    .also { agencyLocation = it }

  fun buildPrison(
    id: String,
    description: String,
    longDescription: String?,
    type: String,
    active: Boolean,
    district: Area?,
    deactivationDate: LocalDate?,
    updateAllowed: Boolean,
    contactName: String?,
    courtTypeCode: String?,
    disabilityAccessCode: String?,
    subArea: Area?,
    area: Area?,
    region: Area?,
    nomsRegion: Area?,
    cjitCode: String?,
    payrollRegionCode: String?,
  ): Prison = Prison(
    id = id,
    description = description,
    longDescription = longDescription,
    type = repository.agencyTypeOf(type),
    active = active,
    district = district,
    deactivationDate = deactivationDate,
    updateAllowed = updateAllowed,
    contactName = contactName,
    courtType = repository.courtTypeOf(courtTypeCode),
    disabilityAccessCode = disabilityAccessCode,
    subArea = subArea,
    area = area,
    region = region,
    nomsRegion = nomsRegion,
    cjitCode = cjitCode,
    payrollRegion = repository.payrollRegionOf(payrollRegionCode),
  )
    .let { repository.savePrison(it) }
    .also { agencyLocation = it }

  override fun address(
    type: String?,
    premise: String?,
    street: String?,
    locality: String?,
    flat: String?,
    postcode: String?,
    city: String?,
    county: String?,
    country: String?,
    validatedPAF: Boolean,
    noFixedAddress: Boolean?,
    primaryAddress: Boolean,
    mailAddress: Boolean,
    comment: String?,
    startDate: String?,
    endDate: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: AgencyLocationAddressDsl.() -> Unit,
  ): AgencyLocationAddress = agencyAddressBuilderFactory.builder().let { builder ->
    builder.build(
      agencyLocation = agencyLocation,
      type = type,
      premise = premise,
      street = street,
      locality = locality,
      flat = flat,
      postcode = postcode,
      city = city,
      county = county,
      country = country,
      validatedPAF = validatedPAF,
      noFixedAddress = noFixedAddress,
      primaryAddress = primaryAddress,
      mailAddress = mailAddress,
      comment = comment,
      startDate = startDate?.let { LocalDate.parse(it) },
      endDate = endDate?.let { LocalDate.parse(it) },
    )
      .also { agencyLocation.addresses += it }
      .also { builder.apply(dsl) }
  }

  override fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    dsl: AgencyLocationPhoneDsl.() -> Unit,
  ): AgencyLocationPhone = agencyPhoneBuilderFactory.builder().let { builder ->
    builder.build(
      agencyLocation = agencyLocation,
      phoneType = phoneType,
      phoneNo = phoneNo,
      extNo = extNo,
    )
      .also { agencyLocation.phones += it }
      .also { builder.apply(dsl) }
  }

  override fun email(
    address: String,
    dsl: AgencyLocationInternetAddressDsl.() -> Unit,
  ): AgencyLocationInternetAddress = agencyLocationInternetAddressBuilderFactory.builder().let { builder ->
    builder.build(
      agencyLocation = agencyLocation,
      internetAddress = address,
    )
      .also { agencyLocation.emailAddresses += it }
      .also { builder.apply(dsl) }
  }

  override fun localAuthority(code: String): AgencyLocationAuthority = repository.localAuthorityTypeOf(code).let {
    AgencyLocationAuthority(id = AgencyLocationAuthorityId(agencyLocation, it.code)).also { authority ->
      agencyLocation.localAuthorities += authority
    }
  }
}
