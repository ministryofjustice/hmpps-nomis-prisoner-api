package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocationAmendment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocationProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.HousingUnitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LivingUnitReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class AgencyInternalLocationDslMarker

@NomisDataDslMarker
interface AgencyInternalLocationDsl {
  @AgencyInternalLocationProfileDslMarker
  fun attributes(
    profileType: String,
    profileCode: String,
  ): AgencyInternalLocationProfile

  @AgencyInternalLocationAmendmentDslMarker
  fun amendments(
    amendDateTime: LocalDateTime,
    columnName: String?,
    oldValue: String? = "old",
    newValue: String? = "new",
    amendUserId: String = "me",
  ): AgencyInternalLocationAmendment
}

@Component
class AgencyInternalLocationBuilderRepository(
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val housingUnitTypeRepository: ReferenceCodeRepository<HousingUnitType>,
  private val livingUnitReasonRepository: ReferenceCodeRepository<LivingUnitReason>,
) {
  fun save(agencyInternalLocation: AgencyInternalLocation): AgencyInternalLocation = agencyInternalLocationRepository.findByIdOrNull(agencyInternalLocation.locationId)
    ?: agencyInternalLocationRepository.save(agencyInternalLocation)

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? = agencyInternalLocationRepository.findByIdOrNull(locationId)

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!

  fun lookupHousingUnitType(code: String): HousingUnitType = housingUnitTypeRepository.findByIdOrNull(HousingUnitType.pk(code))!!

  fun lookupLivingUnitReason(code: String): LivingUnitReason = livingUnitReasonRepository.findByIdOrNull(LivingUnitReason.pk(code))!!
}

@Component
class AgencyInternalLocationBuilderFactory(
  private val repository: AgencyInternalLocationBuilderRepository,
  private val agencyInternalLocationProfileBuilderFactory: AgencyInternalLocationProfileBuilderFactory = AgencyInternalLocationProfileBuilderFactory(),
  private val agencyInternalLocationAmendmentBuilderFactory: AgencyInternalLocationAmendmentBuilderFactory = AgencyInternalLocationAmendmentBuilderFactory(),
) {
  fun builder() = AgencyInternalLocationBuilder(
    repository,
    agencyInternalLocationProfileBuilderFactory,
    agencyInternalLocationAmendmentBuilderFactory,
  )
}

class AgencyInternalLocationBuilder(
  private val repository: AgencyInternalLocationBuilderRepository,
  private val agencyInternalLocationProfileBuilderFactory: AgencyInternalLocationProfileBuilderFactory,
  private val agencyInternalLocationAmendmentBuilderFactory: AgencyInternalLocationAmendmentBuilderFactory,
) : AgencyInternalLocationDsl {
  private lateinit var agencyInternalLocation: AgencyInternalLocation

  fun build(
    locationCode: String,
    locationType: String,
    prisonId: String = "LEI",
    parentAgencyInternalLocationId: Long? = null,
    capacity: Int?,
    operationalCapacity: Int?,
    cnaCapacity: Int?,
    userDescription: String?,
    listSequence: Int?,
    comment: String?,
    active: Boolean,
    deactivationDate: LocalDate?,
    reactivationDate: LocalDate?,
  ): AgencyInternalLocation {
    val parentLocation = parentAgencyInternalLocationId?.let { repository.lookupAgencyInternalLocation(it) }
    return AgencyInternalLocation(
      active = active,
      certified = true,
      tracking = false,
      locationType = locationType,
      agency = repository.lookupAgency(prisonId),
      description = parentLocation?.let { "${it.description}-$locationCode" } ?: "$prisonId-$locationCode",
      parentLocation = parentLocation,
      currentOccupancy = null,
      operationalCapacity = operationalCapacity,
      userDescription = userDescription,
      comment = comment,
      locationCode = locationCode,
      unitType = repository.lookupHousingUnitType("NA"),
      capacity = capacity,
      listSequence = listSequence,
      cnaCapacity = cnaCapacity,
      deactivateDate = deactivationDate,
      reactivateDate = reactivationDate,
      deactivateReason = repository.lookupLivingUnitReason("A"),
    )
      .let { repository.save(it) }
      .also { agencyInternalLocation = it }
  }

  override fun attributes(profileType: String, profileCode: String): AgencyInternalLocationProfile = agencyInternalLocationProfileBuilderFactory.builder().build(profileType, profileCode, agencyInternalLocation)

  override fun amendments(
    amendDateTime: LocalDateTime,
    columnName: String?,
    oldValue: String?,
    newValue: String?,
    amendUserId: String,
  ): AgencyInternalLocationAmendment = agencyInternalLocationAmendmentBuilderFactory.builder().build(
    agencyInternalLocation,
    amendDateTime,
    columnName,
    oldValue,
    newValue,
    amendUserId,
  )
}
