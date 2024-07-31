package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PrisonIepLevel

@Repository
interface PrisonIepLevelRepository : JpaRepository<PrisonIepLevel, PrisonIepLevel.Companion.PK> {
  fun findFirstByAgencyLocationAndIepLevelCode(agencyLocation: AgencyLocation, iepLevelCode: String): PrisonIepLevel?
  fun findFirstByAgencyLocationAndIepLevelCodeAndActive(agencyLocation: AgencyLocation, iepLevelCode: String, active: Boolean = true): PrisonIepLevel?
  fun findAllByAgencyLocationAndActive(agencyLocation: AgencyLocation, active: Boolean = true): List<PrisonIepLevel>
}
