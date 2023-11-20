package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentPartyId

@Repository
interface AdjudicationIncidentPartyRepository :
  JpaRepository<AdjudicationIncidentParty, AdjudicationIncidentPartyId>,
  JpaSpecificationExecutor<AdjudicationIncidentParty> {
  @EntityGraph(type = FETCH, value = "full-adjudication")
  fun findByAdjudicationNumber(adjudicationNumber: Long): AdjudicationIncidentParty?
  fun existsByAdjudicationNumber(adjudicationNumber: Long): Boolean

  @Query(value = "SELECT INCIDENT_ID.nextval FROM dual d", nativeQuery = true)
  fun getNextAdjudicationNumber(): Long
}
