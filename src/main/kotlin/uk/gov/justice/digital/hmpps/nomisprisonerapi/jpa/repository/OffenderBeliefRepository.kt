package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief

@Repository
interface OffenderBeliefRepository : JpaRepository<OffenderBelief, Long> {
  @Query(
    """
      select ob
      from OffenderBelief ob
      join fetch ob.beliefCode
      where ob.rootOffenderId = :rootOffenderId
      order by ob.startDate desc, ob.createDatetime desc
    """,
  )
  fun findBeliefsByRootOffenderId(rootOffenderId: Long): List<OffenderBelief>

  @Query(
    """
      select ob
      from OffenderBelief ob
      join fetch ob.beliefCode
      join Offender o on ob.rootOffenderId = o.id
      where o.nomsId = :prisonNumber
      order by ob.startDate desc, ob.createDatetime desc
    """,
  )
  fun findBeliefsByPrisonNumber(prisonNumber: String): List<OffenderBelief>
}
