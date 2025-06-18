package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PostingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SubAccountType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TransactionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTrustAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.TransactionTypeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderTransactionDslMarker

@NomisDataDslMarker
interface OffenderTransactionDsl

@Component
class OffenderTransactionBuilderRepository(
  private val offenderTransactionRepository: OffenderTransactionRepository,
  private val transactionTypeRepository: TransactionTypeRepository,
  private val trustAccountRepository: OffenderTrustAccountRepository,
) {
  fun lookupTrustAccount(prisonId: String, offenderId: Long): OffenderTrustAccount? = trustAccountRepository
    .findByIdOrNull(OffenderTrustAccount.Companion.Pk(prisonId, offenderId))

  fun lookupTransactionType(type: String): TransactionType = transactionTypeRepository
    .findByIdOrNull(type)!!

  fun save(offenderTransaction: OffenderTransaction): OffenderTransaction = offenderTransactionRepository
    .saveAndFlush(offenderTransaction)

  fun save(offenderTrustAccount: OffenderTrustAccount): OffenderTrustAccount = trustAccountRepository
    .saveAndFlush(offenderTrustAccount)
}

@Component
class OffenderTransactionBuilderFactory(val repository: OffenderTransactionBuilderRepository) {
  fun builder() = OffenderTransactionBuilder(repository)
}

class OffenderTransactionBuilder(
  private val repository: OffenderTransactionBuilderRepository,
) : OffenderTransactionDsl {

  fun build(
    booking: OffenderBooking,
    offender: Offender,
    prisonId: String,
    transactionType: String,
  ): OffenderTransaction = OffenderTransaction(
    offenderBooking = booking,
    transactionEntrySequence = 1,
    trustAccount = lookupOrCreateTrustAccount(prisonId, offender),
    subAccountType = SubAccountType.REG,
    transactionType = repository.lookupTransactionType(transactionType),
    transactionReferenceNumber = "FG1/12",
    clientUniqueRef = "clientUniqueRef",
    entryDate = LocalDate.parse("2025-06-01"),
    entryDescription = "entryDescription",
    entryAmount = BigDecimal.valueOf(2.34),
    modifyDate = LocalDateTime.now(),
    postingType = PostingType.CR,
  )
    .let { repository.save(it) }

  private fun lookupOrCreateTrustAccount(
    prisonId: String,
    offender: Offender,
  ): OffenderTrustAccount = repository
    .lookupTrustAccount(prisonId, offender.id)
    ?: repository.save(
      OffenderTrustAccount(prisonId, offender.id, false, BigDecimal(9.99), BigDecimal(11.11), LocalDateTime.now()),
    )
}
