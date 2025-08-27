package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccountId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PostingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SubAccountType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TransactionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTrustAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.TransactionTypeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

@DslMarker
annotation class OffenderTransactionDslMarker

@NomisDataDslMarker
interface OffenderTransactionDsl {
  @GeneralLedgerTransactionDslMarker
  fun generalLedgerTransaction(
    generalLedgerEntrySequence: Int,
    accountCode: Int,
    dsl: GeneralLedgerTransactionDsl.() -> Unit = {},
  ): GeneralLedgerTransaction
}

@Component
class OffenderTransactionBuilderRepository(
  private val offenderTransactionRepository: OffenderTransactionRepository,
  private val transactionTypeRepository: TransactionTypeRepository,
  private val trustAccountRepository: OffenderTrustAccountRepository,
) {
  fun lookupTrustAccount(prisonId: String, offender: Offender): OffenderTrustAccount? = trustAccountRepository
    .findByIdOrNull(OffenderTrustAccountId(prisonId, offender))

  fun lookupTransactionType(type: String): TransactionType = transactionTypeRepository
    .findByIdOrNull(type)!!

  fun save(offenderTransaction: OffenderTransaction): OffenderTransaction = offenderTransactionRepository
    .saveAndFlush(offenderTransaction)

  fun save(offenderTrustAccount: OffenderTrustAccount): OffenderTrustAccount = trustAccountRepository
    .saveAndFlush(offenderTrustAccount)
}

@Component
class OffenderTransactionBuilderFactory(
  val repository: OffenderTransactionBuilderRepository,
  val generalLedgerTransactionBuilderFactory: GeneralLedgerTransactionBuilderFactory,
) {
  fun builder() = OffenderTransactionBuilder(repository, generalLedgerTransactionBuilderFactory)
}

class OffenderTransactionBuilder(
  private val repository: OffenderTransactionBuilderRepository,
  private val generalLedgerTransactionBuilderFactory: GeneralLedgerTransactionBuilderFactory,
) : OffenderTransactionDsl {
  lateinit var transaction: OffenderTransaction

  fun build(
    transactionId: Long,
    transactionEntrySequence: Int,
    booking: OffenderBooking,
    offender: Offender,
    prisonId: String,
    transactionType: String,
    entryDate: LocalDate,
  ): OffenderTransaction = OffenderTransaction(
    transactionId = transactionId,
    transactionEntrySequence = transactionEntrySequence,
    offenderBooking = booking,
    trustAccount = lookupOrCreateTrustAccount(prisonId, offender),
    subAccountType = SubAccountType.REG,
    transactionType = repository.lookupTransactionType(transactionType),
    transactionReferenceNumber = "FG1/12",
    clientUniqueRef = "clientUniqueRef" + Random.nextInt(),
    entryDate = entryDate,
    entryDescription = "entryDescription",
    entryAmount = BigDecimal.valueOf(2.34),
    modifyDate = LocalDateTime.now(),
    postingType = PostingType.CR,
  )
    .let {
      transaction = repository.save(it)
      return transaction
    }

  override fun generalLedgerTransaction(
    generalLedgerEntrySequence: Int,
    accountCode: Int,
    dsl: GeneralLedgerTransactionDsl.() -> Unit,
  ): GeneralLedgerTransaction = generalLedgerTransactionBuilderFactory.builder().build(
    transaction.transactionId,
    transaction.transactionEntrySequence,
    generalLedgerEntrySequence,
    transaction.trustAccount.id.offender,
    transaction.trustAccount.id.caseloadId,
    transaction.transactionType.type,
    accountCode,
    transaction.postingType,
    transaction.entryDate.atTime(0, 0),
    transaction.transactionReferenceNumber,
    transaction.entryAmount,
  )

  private fun lookupOrCreateTrustAccount(
    prisonId: String,
    offender: Offender,
  ): OffenderTrustAccount = repository
    .lookupTrustAccount(prisonId, offender)
    ?: repository.save(
      OffenderTrustAccount(
        OffenderTrustAccountId(prisonId, offender),
        false,
        BigDecimal(9.99),
        BigDecimal(11.11),
        LocalDateTime.now(),
      ),
    )
}
