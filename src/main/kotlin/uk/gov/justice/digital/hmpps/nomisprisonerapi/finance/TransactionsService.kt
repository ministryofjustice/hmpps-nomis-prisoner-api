package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.GeneralLedgerTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransactionRepository
import java.time.LocalDate

@Service
@Transactional
class TransactionsService(
  val offenderTransactionRepository: OffenderTransactionRepository,
  val generalLedgerTransactionRepository: GeneralLedgerTransactionRepository,
) {
  fun getGeneralLedgerTransactions(
    transactionId: Long,
  ): List<GeneralLedgerTransactionDto> = generalLedgerTransactionRepository
    .findByTransactionId(transactionId)
    .map(::mapGL)

  fun getGeneralLedgerTransactionsForPrison(prisonId: String, date: LocalDate): List<GeneralLedgerTransactionDto> {
    val (minTxnId, maxTxnId) = generalLedgerTransactionRepository.findMinAndMaxTxnIdsByPrisonAndEntryDate(prisonId, date)

    if (minTxnId == null || maxTxnId == null) return emptyList()

    return generalLedgerTransactionRepository
      .findByTransactionsForPrisonBetween(prisonId, minTxnId, maxTxnId)
      .map(::mapGL)
  }

  fun getFirstGeneralLedgerTransactionIdOn(date: LocalDate): Long = generalLedgerTransactionRepository
    .findMinTransactionIdByEntryDate(date)
    ?: throw NotFoundException("No transactions found with date $date")

  fun getGeneralLedgerTransactionsOn(
    transactionId: Long,
  ): List<GeneralLedgerTransactionDto> = generalLedgerTransactionRepository
    .findByTransactionId(transactionId)
    .map(::mapGL)

  fun getOffenderTransactions(transactionId: Long): List<OffenderTransactionDto> = offenderTransactionRepository
    .findByTransactionId(transactionId)
    .map(::mapOT)

  fun findOrphanTransactionsFromId(
    transactionId: Long,
    transactionEntrySequence: Int,
    generalLedgerEntrySequence: Int,
    pageSize: Int,
  ): List<GeneralLedgerTransactionDto> {
    val data = generalLedgerTransactionRepository
      .findNonOffenderByTransactionIdGreaterThan(
        transactionId,
        transactionEntrySequence,
        generalLedgerEntrySequence,
        Limit.of(pageSize),
      )
    return data.map(::mapGL)
  }

  fun findOffenderTransactionsFromId(
    transactionId: Long,
    transactionEntrySequence: Int,
    pageSize: Int,
  ): List<OffenderTransactionDto> {
    val data = offenderTransactionRepository
      .findByTransactionIdGreaterThan(transactionId, transactionEntrySequence, Limit.of(pageSize))
    return data.map(::mapOT)
  }

  fun getPrisonerTransactionIds(
    transactionId: Long,
    pageSize: Int,
    entryDate: LocalDate,
  ): PrisonerTransactionIdsPage = offenderTransactionRepository.findAllPrisonerTransactionIdsWithDateFilter(
    entryDate = entryDate,
    prisonerTransactionId = transactionId,
    pageSize = pageSize,
  )
    .map { PrisonerTransactionIdResponse(transactionId = it.id) }.let { PrisonerTransactionIdsPage(it) }
}

private fun mapOT(transaction: OffenderTransaction): OffenderTransactionDto = OffenderTransactionDto(
  transactionId = transaction.transactionId,
  transactionEntrySequence = transaction.transactionEntrySequence,
  amount = transaction.entryAmount,
  type = transaction.transactionType.type,
  postingType = transaction.postingType,
  offenderNo = transaction.trustAccount.id.offender.nomsId,
  offenderId = transaction.trustAccount.id.offender.id,
  bookingId = transaction.offenderBooking?.bookingId,
  caseloadId = transaction.trustAccount.id.caseloadId,
  entryDate = transaction.entryDate,
  reference = transaction.transactionReferenceNumber,
  clientReference = transaction.clientUniqueRef,
  subAccountType = transaction.subAccountType,
  description = transaction.entryDescription ?: "",
  createdAt = transaction.createDatetime,
  createdBy = transaction.createUsername,
  createdByDisplayName = getDisplayName(transaction.createStaffUserAccount),
  lastModifiedAt = transaction.modifyDatetime,
  lastModifiedBy = transaction.modifyUserId,
  lastModifiedByDisplayName = getDisplayName(transaction.modifyStaffUserAccount),
  generalLedgerTransactions = transaction.generalLedgerTransactions.map(::mapGL),
)

private fun mapGL(gl: GeneralLedgerTransaction): GeneralLedgerTransactionDto = GeneralLedgerTransactionDto(
  transactionId = gl.transactionId,
  transactionEntrySequence = gl.transactionEntrySequence,
  generalLedgerEntrySequence = gl.generalLedgerEntrySequence,
  caseloadId = gl.caseloadId,
  amount = gl.entryAmount,
  type = gl.transactionType.type,
  postingType = gl.postUsage,
  accountCode = gl.accountCode.accountCode,
  description = gl.entryDescription ?: "",
  transactionTimestamp = gl.entryDate.atTime(gl.entryTime),
  reference = gl.transactionReferenceNumber,
  createdAt = gl.createDatetime,
  createdBy = gl.createUsername,
  createdByDisplayName = getDisplayName(gl.createStaffUserAccount),
  lastModifiedAt = gl.modifyDatetime,
  lastModifiedBy = gl.modifyUserId,
  lastModifiedByDisplayName = getDisplayName(gl.modifyStaffUserAccount),
)

private fun getDisplayName(staffUserAccount: StaffUserAccount?): String = staffUserAccount
  ?.staff?.run { "$firstName $lastName" } ?: "Unknown"
