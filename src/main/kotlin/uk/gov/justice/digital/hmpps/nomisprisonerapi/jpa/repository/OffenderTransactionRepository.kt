package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction.Companion.Pk

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
}
