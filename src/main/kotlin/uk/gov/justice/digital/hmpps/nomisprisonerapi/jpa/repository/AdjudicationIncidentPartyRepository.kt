package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentPartyId

@Repository
interface AdjudicationIncidentPartyRepository :
  JpaRepository<AdjudicationIncidentParty, AdjudicationIncidentPartyId>,
  JpaSpecificationExecutor<AdjudicationIncidentParty> {
  fun findByAdjudicationNumber(adjudicationNumber: Long): AdjudicationIncidentParty?
  fun existsByAdjudicationNumber(adjudicationNumber: Long): Boolean
}
