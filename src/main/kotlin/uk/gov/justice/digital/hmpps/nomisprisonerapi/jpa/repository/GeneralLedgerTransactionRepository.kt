package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction.Companion.Pk

@Repository
interface GeneralLedgerTransactionRepository : JpaRepository<GeneralLedgerTransaction, Pk> {
  fun findByTransactionId(id: Long): List<GeneralLedgerTransaction>
}
