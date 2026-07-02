package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType

@Repository
interface AgencyLocationRepository :
  JpaRepository<AgencyLocation, String>,
  JpaSpecificationExecutor<AgencyLocation> {
  fun findByTypeAndActiveAndDeactivationDateIsNullAndIdNotInOrderById(
    type: AgencyLocationType,
    active: Boolean,
    ignoreList: List<String> = listOf(),
  ): List<AgencyLocation>
}
