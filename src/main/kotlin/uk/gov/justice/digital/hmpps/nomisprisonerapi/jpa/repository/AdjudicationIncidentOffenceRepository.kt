package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentOffence

@Repository
interface AdjudicationIncidentOffenceRepository : JpaRepository<AdjudicationIncidentOffence, Long> {
  fun findByCode(code: String): AdjudicationIncidentOffence?
}
