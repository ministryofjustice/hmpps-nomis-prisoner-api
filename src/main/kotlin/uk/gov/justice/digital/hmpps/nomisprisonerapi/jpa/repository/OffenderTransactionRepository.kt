package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction.Companion.Pk

@Repository
interface OffenderTransactionRepository : JpaRepository<OffenderTransaction, Pk> {
  fun findByTransactionId(id: Long): List<OffenderTransaction>
}
