package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AvailablePrisonIepLevel

@Repository
interface AvailablePrisonIepLevelRepository : JpaRepository<AvailablePrisonIepLevel, AvailablePrisonIepLevel.Companion.PK> {
  fun findFirstByAgencyLocationAndId(agencyLocation: AgencyLocation, id: String): AvailablePrisonIepLevel?
  fun findFirstByAgencyLocationAndIdAndActive(agencyLocation: AgencyLocation, id: String, active: Boolean = true): AvailablePrisonIepLevel?
  fun findAllByAgencyLocationAndActive(agencyLocation: AgencyLocation, active: Boolean = true): List<AvailablePrisonIepLevel>
}
