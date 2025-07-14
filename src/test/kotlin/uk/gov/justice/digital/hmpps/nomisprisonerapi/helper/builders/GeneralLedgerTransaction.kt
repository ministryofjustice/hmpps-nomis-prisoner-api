package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

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
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class GeneralLedgerTransactionDslMarker

@NomisDataDslMarker
interface GeneralLedgerTransactionDsl

@Component
class GeneralLedgerTransactionBuilderRepository(
  private val generalLedgerTransactionRepository: GeneralLedgerTransactionRepository,
  private val transactionTypeRepository: TransactionTypeRepository,
  private val accountCodeRepository: AccountCodeRepository,
) {
  fun lookupAccountCode(code: Int): AccountCode = accountCodeRepository
    .findById(code).orElseThrow { NotFoundException("not found: accountCode=$code") }

  fun lookupTransactionType(type: String): TransactionType = transactionTypeRepository
    .findById(type).orElseThrow { NotFoundException("not found: transactionType=$type") }

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
    accountPeriodId = 202507,
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
    createDate = LocalDate.now(),
  )
    .let { repository.save(it) }
}
