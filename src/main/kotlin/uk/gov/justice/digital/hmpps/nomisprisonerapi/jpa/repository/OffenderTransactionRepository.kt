package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction.Companion.Pk
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

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
            ot.txn_id > :prisonerTransactionId
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

  @Query(
    """
      select * from 
        (select distinct(ot.txn_id) as id
          from offender_transactions ot
          where 
            ot.txn_id > :minTxnId
            and ot.txn_id <= :maxTxnId
          order by ot.txn_id
        )
      where rownum <= :pageSize
  """,
    nativeQuery = true,
  )
  fun findTransactionIdsInRange(minTxnId: Long, maxTxnId: Long, pageSize: Int): List<PrisonerTransactionIdProjection>

  @Query(
    """
      select 
        ot.txn_id txnId,
        ot.txn_entry_seq txnEntrySeq,
        ot.txn_entry_date txnEntryDate,
        ot.txn_entry_desc txnEntryDesc,
        ot.txn_reference_number txnReferenceNumber,
        ot.txn_entry_amount txnEntryAmount,
        ot.hold_number holdNumber,
        ot.client_unique_ref as clientUniqueRef
    from offender_transactions ot
    where ot.offender_id = :offenderId
      and ot.txn_type in ('HOA', 'WHF')
      and ot.hold_clear_flag = 'N'
      and ot.hold_number is not null
    order by ot.txn_id, ot.txn_entry_seq
  """,
    nativeQuery = true,
  )
  fun getHoldTransactions(offenderId: Long): List<PrisonerHoldProjection>
}

interface PrisonerTransactionIdProjection {
  val id: Long
}

interface PrisonerHoldProjection {
  val txnId: Long
  val txnEntrySeq: Int
  val txnEntryDate: LocalDateTime
  val txnEntryDesc: String?
  val txnReferenceNumber: String?
  val txnEntryAmount: BigDecimal
  val holdNumber: Long?
  val clientUniqueRef: String?
}
