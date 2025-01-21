package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief

@Repository
interface OffenderBeliefRepository : JpaRepository<OffenderBelief, Long> {
  fun findByRootOffenderOrderByStartDateDesc(rootOffender: Offender): List<OffenderBelief>
}
