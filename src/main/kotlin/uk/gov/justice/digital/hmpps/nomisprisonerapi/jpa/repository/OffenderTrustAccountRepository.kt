package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccountId

@Repository
interface OffenderTrustAccountRepository : JpaRepository<OffenderTrustAccount, OffenderTrustAccountId> {
  @Query(
    """
        select
            distinct(ota.id.offender.id) as offenderId
        from 
            OffenderTrustAccount ota
        where 
            (ota.currentBalance != 0 or ota.holdBalance != 0)
            and
            (:prisonIds is null or ota.id.caseloadId in :prisonIds)
    """,
  )
  fun findAllOffenderIdsWithBalances(prisonIds: List<String>?, pageable: Pageable): Page<Long>

  @Query(
    """
      select * from (
        select
            distinct OFFENDER_ID
         from 
            OFFENDER_TRUST_ACCOUNTS 
         where 
            OFFENDER_ID > :offenderId and
            (CURRENT_BALANCE != 0 or HOLD_BALANCE != 0)
         order by OFFENDER_ID) 
      where rownum <= :pageSize
  """,
    nativeQuery = true,
  )
  fun findAllOffendersIdsWithBalancesFromId(offenderId: Long, pageSize: Int): List<Long>

  @Query(
    """
      select * from (
        select
            distinct OFFENDER_ID
         from 
            OFFENDER_TRUST_ACCOUNTS 
         where 
            OFFENDER_ID > :offenderId and
            (CURRENT_BALANCE != 0 or HOLD_BALANCE != 0) and
            CASELOAD_ID in (:prisonIds)
         order by OFFENDER_ID) 
      where rownum <= :pageSize
  """,
    nativeQuery = true,
  )
  fun findAllOffendersIdsWithBalancesFromId(offenderId: Long, prisonIds: List<String>, pageSize: Int): List<Long>

  @Query(
    """
     select distinct(ota.id.offender.id) as offenderId
        from OffenderTrustAccount ota
        where (ota.currentBalance != 0 or ota.holdBalance != 0)
          and (:prisonIds is null or ota.id.caseloadId in :prisonIds)
          and ota.id.offender.id > :fromRootOffenderId and ota.id.offender.id <= :toRootOffenderId
        order by ota.id.offender.id
  """,
  )
  fun findAllOffendersIdsWithBalancesBetweenIds(fromRootOffenderId: Long, toRootOffenderId: Long, prisonIds: List<String>?): List<Long>

  @Query(
    """
         select OFFENDER_ID from (
           select OFFENDER_ID, rownum as seqnum from (
             select distinct OFFENDER_ID
             from OFFENDER_TRUST_ACCOUNTS 
             where CURRENT_BALANCE != 0 or HOLD_BALANCE != 0
             order by OFFENDER_ID
           )
        ) where mod(seqnum, :pageSize) = 0
  """,
    nativeQuery = true,
  )
  fun findEveryPageSizeOffenderIdWithBalance(pageSize: Int): List<Long>

  @Query(
    """
         select OFFENDER_ID from (
           select OFFENDER_ID, rownum as seqnum from (
             select distinct OFFENDER_ID
             from OFFENDER_TRUST_ACCOUNTS 
             where (CURRENT_BALANCE != 0 or HOLD_BALANCE != 0)
             and   CASELOAD_ID in (:prisonIds)
             order by OFFENDER_ID
           )
        ) where mod(seqnum, :pageSize) = 0
  """,
    nativeQuery = true,
  )
  fun findEveryPageSizeOffenderIdWithBalance(prisonIds: List<String>, pageSize: Int): List<Long>
}
