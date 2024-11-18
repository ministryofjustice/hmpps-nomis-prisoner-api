package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import java.util.Optional

@Repository
interface AgencyInternalLocationRepository : JpaRepository<AgencyInternalLocation, Long> {

  fun findOneByDescription(description: String): Optional<AgencyInternalLocation>
  fun findByDescriptionAndAgencyId(description: String, agencyId: String): AgencyInternalLocation?
  fun findByAgencyIdAndActiveAndLocationCodeInAndParentLocationIsNull(
    agencyId: String,
    active: Boolean = true,
    locationCodes: List<String> = listOf("VISIT", "VISITS"),
  ): AgencyInternalLocation?

  fun findAgencyInternalLocationsByAgencyIdAndLocationTypeAndActive(
    agencyId: String,
    locationType: String,
    active: Boolean = true,
  ): List<AgencyInternalLocation>

  fun findByAgencyIdAndLocationCodeAndActive(agencyId: String, locationCode: String, active: Boolean): AgencyInternalLocation?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")])
  fun findWithLockByLocationId(locationId: Long): AgencyInternalLocation?
}
