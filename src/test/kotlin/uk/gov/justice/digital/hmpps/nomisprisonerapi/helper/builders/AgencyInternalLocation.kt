package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType.Companion.pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class AgencyInternalLocationDslMarker

@NomisDataDslMarker
interface AgencyInternalLocationDsl {

}

@Component
class AgencyInternalLocationBuilderRepository(
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val internalLocationTypeRepository: ReferenceCodeRepository<InternalLocationType>,
) {
  fun save(agencyInternalLocation: AgencyInternalLocation): AgencyInternalLocation =
    agencyInternalLocationRepository.findByIdOrNull(agencyInternalLocation.locationId)
      ?: agencyInternalLocationRepository.save(agencyInternalLocation)

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? =
    agencyInternalLocationRepository.findByIdOrNull(locationId)

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!

  fun lookupInternalLocationType(code: String): InternalLocationType = internalLocationTypeRepository.findByIdOrNull(pk(code))!!
}

@Component
class AgencyInternalLocationBuilderFactory(
  private val repository: AgencyInternalLocationBuilderRepository,
  private val serviceAgencySwitchBuilderFactory: ServiceAgencySwitchBuilderFactory,
) {
  fun builder() = AgencyInternalLocationBuilder(repository, serviceAgencySwitchBuilderFactory)
}

class AgencyInternalLocationBuilder(
  private val repository: AgencyInternalLocationBuilderRepository,
  private val serviceAgencySwitchBuilderFactory: ServiceAgencySwitchBuilderFactory,
) : AgencyInternalLocationDsl {
  private lateinit var agencyInternalLocation: AgencyInternalLocation

  fun build(
    locationCode: String,
    locationType: String,
    prisonId: String = "LEI",
    parentAgencyInternalLocationId: Long? = null,
  ): AgencyInternalLocation = AgencyInternalLocation(
    active = true,
    certified = true,
    tracking = false,
    locationType = repository.lookupInternalLocationType(locationType),
    agency = repository.lookupAgency(prisonId),
    description = "Description",
    parentLocation = parentAgencyInternalLocationId?.let { repository.lookupAgencyInternalLocation(it) },
    currentOccupancy = null,
    operationalCapacity = null,
    userDescription = null,
    comment = null,
    locationCode = locationCode,
    unitType = null,
    capacity = null,
    listSequence = null,
    cnaCapacity = null,
  )
    .let { repository.save(it) }
    .also { agencyInternalLocation = it }

}
