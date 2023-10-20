package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing

@Repository
interface AdjudicationHearingRepository : JpaRepository<AdjudicationHearing, Long> {
  @EntityGraph(type = FETCH, value = "full-hearing")
  fun findByAdjudicationNumber(adjudicationNumber: Long): List<AdjudicationHearing>
  fun deleteByAdjudicationNumber(adjudicationNumber: Long)
  fun findByAdjudicationNumberAndComment(adjudicationNumber: Long, comment: String): AdjudicationHearing?
}
