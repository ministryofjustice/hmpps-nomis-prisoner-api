package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import java.util.Optional

@Repository
interface AgencyInternalLocationRepository : JpaRepository<AgencyInternalLocation, Long> {
  fun findAgencyInternalLocationsByAgencyIdAndLocationTypeAndActive(
    agencyId: String,
    locationType: String,
    active: Boolean
  ): List<AgencyInternalLocation>

  fun findAgencyInternalLocationsByAgencyIdAndLocationType(
    agencyId: String,
    locationType: String
  ): List<AgencyInternalLocation>

  fun findOneByDescription(description: String): Optional<AgencyInternalLocation>
  fun findOneByDescriptionAndAgencyId(description: String, agencyId: String): Optional<AgencyInternalLocation>
  fun findByDescriptionAndAgencyId(description: String, agencyId: String): AgencyInternalLocation?
  fun findOneByLocationId(locationId: Long): Optional<AgencyInternalLocation>
  fun findByLocationCodeAndAgencyId(locationCode: String, agencyId: String): List<AgencyInternalLocation>
  fun findByAgencyIdAndLocationTypeAndActiveAndParentLocationIsNull(
    agencyId: String,
    locationType: String,
    active: Boolean
  ): List<AgencyInternalLocation>
}
