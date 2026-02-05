package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import java.util.Optional

@Repository
interface AgencyLocationRepository :
  JpaRepository<AgencyLocation, String>,
  JpaSpecificationExecutor<AgencyLocation> {
  fun findByIdAndDeactivationDateIsNull(id: String): Optional<AgencyLocation>
  fun findByIdAndTypeAndActiveAndDeactivationDateIsNull(
    id: String,
    type: AgencyLocationType,
    active: Boolean,
  ): Optional<AgencyLocation>

  fun findByTypeAndActiveAndDeactivationDateIsNullAndIdNotInOrderById(
    type: AgencyLocationType,
    active: Boolean,
    ignoreList: List<String> = listOf(),
  ): List<AgencyLocation>
}
