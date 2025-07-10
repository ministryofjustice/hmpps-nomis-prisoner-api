package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AccountCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PostingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TransactionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AccountCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.GeneralLedgerTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.TransactionTypeRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@DslMarker
annotation class GeneralLedgerTransactionDslMarker

@NomisDataDslMarker
interface GeneralLedgerTransactionDsl {
//  @GeneralLedgerTransactionDslMarker
//  fun generalLedgerTransaction(
//    transactionId: Long,
//    transactionEntrySequence: Int,
//    generalLedgerEntrySequence: Int,
//    offender: Offender? = null,
//    caseloadId: String,
//    transactionType: String,
//    accountCode: Int = 10201,
//    postUsage: PostingType = PostingType.CR,
//    entryDateTime: LocalDateTime = LocalDateTime.parse("2025-06-01T12:13:14"),
//    transactionReferenceNumber: String? = "FG1/12",
//    entryAmount: BigDecimal = BigDecimal.valueOf(2.34),
//    dsl: GeneralLedgerTransactionDsl.() -> Unit = {},
//  ): GeneralLedgerTransaction
}

@Component
class GeneralLedgerTransactionBuilderRepository(
  private val generalLedgerTransactionRepository: GeneralLedgerTransactionRepository,
  private val transactionTypeRepository: TransactionTypeRepository,
  private val accountCodeRepository: AccountCodeRepository,
) {
  fun lookupAccountCode(code: Int): AccountCode = accountCodeRepository
    .findById(code).orElseThrow { NotFoundException("not found: accountCode=$code") }

  fun lookupTransactionType(type: String): TransactionType = transactionTypeRepository
    .findByIdOrNull(type)!!

  fun save(
    generalLedgerTransaction: GeneralLedgerTransaction,
  ): GeneralLedgerTransaction = generalLedgerTransactionRepository
    .saveAndFlush(generalLedgerTransaction)
}

@Component
class GeneralLedgerTransactionBuilderFactory(
  val repository: GeneralLedgerTransactionBuilderRepository,
) {
  fun builder() = GeneralLedgerTransactionBuilder(repository)
}

class GeneralLedgerTransactionBuilder(
  private val repository: GeneralLedgerTransactionBuilderRepository,
) : GeneralLedgerTransactionDsl {
  fun build(
    transactionId: Long,
    transactionEntrySequence: Int,
    generalLedgerEntrySequence: Int,
    offender: Offender?,
    caseloadId: String,
    transactionType: String,
    accountCode: Int,
    postUsage: PostingType,
    entryDateTime: LocalDateTime,
    transactionReferenceNumber: String?,
    entryAmount: BigDecimal,
  ): GeneralLedgerTransaction = GeneralLedgerTransaction(
    transactionId = transactionId,
    transactionEntrySequence = transactionEntrySequence,
    generalLedgerEntrySequence = generalLedgerEntrySequence,
    transactionType = repository.lookupTransactionType(transactionType),
    caseloadId = caseloadId,
    offenderId = offender?.id,
    transactionReferenceNumber = transactionReferenceNumber,
    entryDate = entryDateTime.toLocalDate(),
    entryTime = entryDateTime.toLocalTime(),
    entryDescription = "entry description",
    entryAmount = entryAmount,
    accountCode = repository.lookupAccountCode(accountCode),
    postUsage = postUsage,
  )
    .let { repository.save(it) }

//  override fun generalLedgerTransaction(
//    transactionId: Long,
//    transactionEntrySequence: Int,
//    generalLedgerEntrySequence: Int,
//    offender: Offender?,
//    caseloadId: String,
//    transactionType: String,
//    accountCode: Int,
//    postUsage: PostingType,
//    entryDateTime: LocalDateTime,
//    transactionReferenceNumber: String?,
//    entryAmount: BigDecimal,
//    dsl: GeneralLedgerTransactionDsl.() -> Unit,
//  ): GeneralLedgerTransaction = generalLedgerTransactionBuilderFactory.builder().build(
//    transactionId,
//    transactionEntrySequence,
//    generalLedgerEntrySequence,
//    offender,
//    caseloadId,
//    transactionType,
//    accountCode,
//    postUsage,
//    entryDateTime,
//    transactionReferenceNumber,
//    entryAmount,
//  )
}
