package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderRestrictions
import java.time.LocalDateTime

@Repository
interface OffenderRestrictionsRepository : JpaRepository<OffenderRestrictions, Long> {
  @Query(
    """
      select 
        r.id as restrictionId
      from OffenderRestrictions r 
    """,
  )
  fun findAllIds(
    pageable: Pageable,
  ): Page<RestrictionIdProjection>

  @Query(
    """
      select 
        r.id as restrictionId
      from OffenderRestrictions r 
        where 
          (:fromDate is null or r.createDatetime >= :fromDate) and 
          (:toDate is null or r.createDatetime <= :toDate) 
    """,
  )
  fun findAllIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<RestrictionIdProjection>
}

interface RestrictionIdProjection {
  val restrictionId: Long
}
