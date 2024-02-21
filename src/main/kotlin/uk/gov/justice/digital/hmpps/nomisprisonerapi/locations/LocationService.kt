package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.HousingUnitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LivingUnitReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@Service
@Transactional
class LocationService(
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val internalLocationTypeRepository: ReferenceCodeRepository<InternalLocationType>,
  private val housingUnitTypeRepository: ReferenceCodeRepository<HousingUnitType>,
  private val livingUnitReasonRepository: ReferenceCodeRepository<LivingUnitReason>,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Audit
  fun createLocation(locationDto: CreateLocationRequest): LocationIdResponse {
    val locationType = internalLocationTypeRepository.findByIdOrNull(InternalLocationType.pk(locationDto.locationType))
      ?: throw BadDataException("Location type with id=${locationDto.locationType} does not exist")

    val housingUnitType = locationDto.unitType?.let {
      housingUnitTypeRepository.findByIdOrNull(HousingUnitType.pk(it))
        ?: throw BadDataException("Housing unit type with id=${locationDto.unitType} does not exist")
    }

    val agency = agencyLocationRepository.findByIdOrNull(locationDto.prisonId)
      ?: throw BadDataException("Agency with id=${locationDto.prisonId} does not exist")

    val parent = locationDto.parentLocationId?.let {
      agencyInternalLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Parent location with id=$it does not exist")
    }

    return LocationIdResponse(
      agencyInternalLocationRepository.save(
        locationDto.toAgencyInternalLocation(locationType, housingUnitType, agency, parent),
      ).also {
        telemetryClient.trackEvent(
          "location-created",
          mapOf(
            "locationId" to it.locationId.toString(),
            "description" to it.description,
          ),
          null,
        )
      }.locationId,
    )
  }

  @Audit
  fun updateLocation(locationId: Long, locationDto: UpdateLocationRequest) {
    val location = agencyInternalLocationRepository.findByIdOrNull(locationId)
      ?: throw NotFoundException("Location with id=$locationId does not exist")

    val internalLocationType =
      internalLocationTypeRepository.findByIdOrNull(InternalLocationType.pk(locationDto.locationType))
        ?: throw BadDataException("Location type with id=${locationDto.locationType} does not exist")

    val housingUnitType = locationDto.unitType?.let {
      housingUnitTypeRepository.findByIdOrNull(HousingUnitType.pk(it))
        ?: throw BadDataException("Housing unit type with id=${locationDto.unitType} does not exist")
    }

    val parent = locationDto.parentLocationId?.let {
      agencyInternalLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Parent location with id=$it does not exist")
    }

    location.apply {
      description =
        location.parentLocation?.let { "${location.parentLocation!!.description}-${locationDto.locationCode}" }
          ?: locationDto.locationCode
      locationType = internalLocationType
      description = locationDto.description
      userDescription = locationDto.userDescription
      locationCode = locationDto.locationCode
      parentLocation = parent
      listSequence = locationDto.listSequence
      comment = locationDto.comment
      unitType = housingUnitType
    }.also {
      telemetryClient.trackEvent(
        "location-updated",
        mapOf(
          "locationId" to it.locationId.toString(),
          "description" to it.description,
        ),
        null,
      )
    }
  }

  @Audit
  fun deactivateLocation(locationId: Long, deactivateRequest: DeactivateRequest) {
    val location = agencyInternalLocationRepository.findByIdOrNull(locationId)
      ?: throw NotFoundException("Location with id=$locationId does not exist")

    if (!location.active) {
      throw BadDataException("Location with id=$locationId is already inactive")
    }

    location.deactivateDate = LocalDate.now()
    location.deactivateReason = deactivateRequest.reasonCode?.let {
      livingUnitReasonRepository.findByIdOrNull(LivingUnitReason.pk(it))
        ?: throw BadDataException("Deactivate Reason code=$it does not exist")
    }
    location.reactivateDate = deactivateRequest.reactivateDate
    location.active = false

    telemetryClient.trackEvent(
      "location-deactivated",
      mapOf(
        "locationId" to location.locationId.toString(),
        "description" to location.description,
      ),
      null,
    )
  }

  @Audit
  fun reactivateLocation(locationId: Long) {
    val location = agencyInternalLocationRepository.findByIdOrNull(locationId)
      ?: throw NotFoundException("Location with id=$locationId does not exist")

    if (location.active) {
      throw BadDataException("Location with id=$locationId is already active")
    }
    location.deactivateDate = null
    location.deactivateReason = null
    location.reactivateDate = null
    location.active = true

    telemetryClient.trackEvent(
      "location-reactivated",
      mapOf(
        "locationId" to location.locationId.toString(),
        "description" to location.description,
      ),
      null,
    )
  }

  @Audit
  fun updateCapacity(locationId: Long, capacity: UpdateCapacityRequest) {
    val location = agencyInternalLocationRepository.findByIdOrNull(locationId)
      ?: throw NotFoundException("Location with id=$locationId does not exist")

    location.capacity = capacity.capacity
    location.operationalCapacity = capacity.operationalCapacity

    // Note that parent capacities are done by separate sync event

    telemetryClient.trackEvent(
      "location-capacity-changed",
      mapOf(
        "locationId" to location.locationId.toString(),
        "description" to location.description,
        "capacity" to location.capacity.toString(),
        "operationalCapacity" to location.operationalCapacity.toString(),
      ),
      null,
    )
  }

  @Audit
  fun updateCertification(locationId: Long, certification: UpdateCertificationRequest) {
    val location = agencyInternalLocationRepository.findByIdOrNull(locationId)
      ?: throw NotFoundException("Location with id=$locationId does not exist")

    location.certified = certification.certified
    location.cnaCapacity = certification.cnaCapacity

    // Note that parent capacities are done by separate sync event

    telemetryClient.trackEvent(
      "location-certification-changed",
      mapOf(
        "locationId" to location.locationId.toString(),
        "description" to location.description,
        "cnaCapacity" to location.cnaCapacity.toString(),
        "certified" to location.certified.toString(),
      ),
      null,
    )
  }

  fun getLocation(id: Long): LocationResponse =
    agencyInternalLocationRepository.findByIdOrNull(id)?.toLocationResponse()
      ?: throw NotFoundException("Location with id=$id does not exist")

  fun getLocationByKey(key: String): LocationResponse = agencyInternalLocationRepository.findOneByDescription(key)
    .orElseThrow(NotFoundException("Location with business key=$key does not exist"))
    .toLocationResponse()

  fun findIdsByFilter(pageRequest: Pageable): Page<LocationIdResponse> {
    log.info("Location Id request with page request $pageRequest")
    return agencyInternalLocationRepository.findAll(pageRequest)
      .map { LocationIdResponse(it.locationId) }
  }

  private fun AgencyInternalLocation.toLocationResponse(): LocationResponse =
    LocationResponse(
      locationId = locationId,
      description = description,
      locationType = locationType.code,
      prisonId = agency.id,
      parentLocationId = parentLocation?.locationId,
      operationalCapacity = operationalCapacity,
      cnaCapacity = cnaCapacity,
      userDescription = userDescription,
      locationCode = locationCode,
      capacity = capacity,
      listSequence = listSequence,
      comment = comment,
    )
}
