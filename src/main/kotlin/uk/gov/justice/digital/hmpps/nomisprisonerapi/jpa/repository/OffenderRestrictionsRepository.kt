package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderRestrictions

@Repository
interface OffenderRestrictionsRepository : JpaRepository<OffenderRestrictions, Long> {

  @Query(
    """
      select 
       OFFENDER_RESTRICTION_ID 
      from OFFENDER_RESTRICTIONS
      where  OFFENDER_RESTRICTION_ID > :restrictionId
        and rownum <= :pageSize
      order by OFFENDER_RESTRICTION_ID
    """,
    nativeQuery = true,
  )
  fun findAllIdsFromId(restrictionId: Long, pageSize: Int): List<Long>
}
