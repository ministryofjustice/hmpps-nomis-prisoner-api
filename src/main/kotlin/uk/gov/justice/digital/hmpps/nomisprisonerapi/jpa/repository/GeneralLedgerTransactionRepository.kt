package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction.Companion.Pk
import java.math.BigDecimal
import java.time.LocalDate

interface GeneralLedgerTransactionRepository : JpaRepository<GeneralLedgerTransaction, Pk> {
  fun findByTransactionId(id: Long): List<GeneralLedgerTransaction>

  @Query("select min(transactionId) from GeneralLedgerTransaction where entryDate = :date")
  fun findMinTransactionIdByEntryDate(date: LocalDate): Long?

  @Query(
    """
    select min(txn_id) as minTxnId,
           max(txn_id) as maxTxnId
    from gl_transactions
    where
      caseload_id = :prisonId
      and offender_id is null
      and txn_entry_date = :entryDate
   """,
    nativeQuery = true,
  )
  fun findMinAndMaxTxnIdsByPrisonAndEntryDate(prisonId: String, entryDate: LocalDate): Pair<BigDecimal?, BigDecimal?>

  @Query(
    """
    select *
    from gl_transactions gl
    where  
      gl.txn_id between :minTxnId and :maxTxnId
      and gl.caseload_id = :prisonId
      and gl.offender_id is null
    """,
    nativeQuery = true,
  )
  fun findByTransactionsForPrisonBetween(prisonId: String, minTxnId: BigDecimal, maxTxnId: BigDecimal): List<GeneralLedgerTransaction>

  @Query(
    """
      select gl
      from GeneralLedgerTransaction gl
        left join OffenderTransaction ot on gl.transactionId = ot.transactionId and gl.transactionEntrySequence = ot.transactionEntrySequence
      where (gl.transactionId > :transactionId
            or (gl.transactionId = :transactionId and gl.transactionEntrySequence > :transactionEntrySequence)
            or (gl.transactionId = :transactionId and gl.transactionEntrySequence = :transactionEntrySequence and gl.generalLedgerEntrySequence > :generalLedgerEntrySequence)
          )
        and ot.transactionId is null
      order by gl.transactionId, gl.transactionEntrySequence, gl.generalLedgerEntrySequence
    """,
  )
  fun findNonOffenderByTransactionIdGreaterThan(
    transactionId: Long,
    transactionEntrySequence: Int,
    generalLedgerEntrySequence: Int,
    limit: Limit,
  ): List<GeneralLedgerTransaction>
}
