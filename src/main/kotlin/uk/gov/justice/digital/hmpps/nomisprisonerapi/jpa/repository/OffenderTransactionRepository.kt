package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction.Companion.Pk
import java.time.LocalDate

@Repository
interface OffenderTransactionRepository : JpaRepository<OffenderTransaction, Pk> {
  fun findByTransactionId(id: Long): List<OffenderTransaction>

  @Query(
    """
      from OffenderTransaction ot
      where ot.transactionId > :transactionId
        or (ot.transactionId = :transactionId and ot.transactionEntrySequence > :transactionEntrySequence)
      order by ot.transactionId
    """,
  )
  fun findByTransactionIdGreaterThan(
    transactionId: Long,
    transactionEntrySequence: Int,
    limit: Limit,
  ): List<OffenderTransaction>

  @Query(
    """
      select * from 
        (select distinct(ot.txn_id) as id
          from offender_transactions ot
          where 
            ot.txn_id > case :prisonerTransactionId 
                          when 0 
                            then (select min(txn_id)-1 from gl_transactions where txn_entry_date = :entryDate) 
                          else :prisonerTransactionId
                        end
            and ot.txn_id <= (select max(txn_id) from gl_transactions where txn_entry_date = :entryDate)
          order by ot.txn_id
        )
      where rownum <= :pageSize
  """,
    nativeQuery = true,
  )
  fun findAllPrisonerTransactionIdsWithDateFilter(
    entryDate: LocalDate,
    prisonerTransactionId: Long,
    pageSize: Int,
  ): List<PrisonerTransactionIdProjection>
}

interface PrisonerTransactionIdProjection {
  val id: Long
}
