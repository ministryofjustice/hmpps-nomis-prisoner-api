package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@Service
@Transactional
class LocationService(
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val internalLocationTypeRepository: ReferenceCodeRepository<InternalLocationType>,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createLocation(locationDto: CreateLocationRequest): LocationIdResponse {
    val locationType = internalLocationTypeRepository.findByIdOrNull(InternalLocationType.pk(locationDto.locationType))
      ?: throw BadDataException("Location type with id=${locationDto.locationType} does not exist")

    val agency = agencyLocationRepository.findByIdOrNull(locationDto.prisonId)
      ?: throw BadDataException("Agency with id=${locationDto.prisonId} does not exist")

    val parent = locationDto.parentLocationId?.let {
      agencyInternalLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Parent location with id=$it does not exist")
    }

    return LocationIdResponse(
      agencyInternalLocationRepository.save(
        locationDto.toAgencyInternalLocation(locationType, agency, parent),
      ).locationId,
    )
  }

  fun getLocation(id: Long): LocationResponse {
    return agencyInternalLocationRepository.findByIdOrNull(id)?.toLocationResponse()
      ?: throw NotFoundException("Location with id=$id does not exist")
  }

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
