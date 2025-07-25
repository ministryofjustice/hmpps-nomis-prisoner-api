package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PostingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SubAccountType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.GeneralLedgerTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransactionRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class TransactionsService(
  val offenderTransactionRepository: OffenderTransactionRepository,
  val generalLedgerTransactionRepository: GeneralLedgerTransactionRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getOffenderTransactions(transactionId: Long): List<OffenderTransactionDto> = offenderTransactionRepository
    .findByTransactionId(transactionId)
    .map {
      OffenderTransactionDto(
        transactionId = it.transactionId,
        transactionEntrySequence = it.transactionEntrySequence,
        amount = it.entryAmount,
        type = it.transactionType.type,
        postingType = it.postingType,
        offenderNo = it.trustAccount.id.offender.nomsId,
        offenderId = it.trustAccount.id.offender.id,
        bookingId = it.offenderBooking?.bookingId,
        caseloadId = it.trustAccount.id.caseloadId,
        entryDate = it.entryDate,
        reference = it.transactionReferenceNumber,
        clientReference = it.clientUniqueRef,
        subAccountType = it.subAccountType,
        description = it.entryDescription ?: "",
        createdAt = it.createDatetime,
        createdBy = it.createUsername,
        createdByDisplayName = getDisplayName(it.createStaffUserAccount),
        lastModifiedAt = it.modifyDatetime,
        lastModifiedBy = it.modifyUserId,
        lastModifiedByDisplayName = getDisplayName(it.modifyStaffUserAccount),
        generalLedgerTransactions = it.generalLedgerTransactions.map(::mapGL),
      )
    }

  fun getGeneralLedgerTransactions(
    transactionId: Long,
  ): List<GeneralLedgerTransactionDto> = generalLedgerTransactionRepository
    .findByTransactionId(transactionId)
    .map(::mapGL)
}

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

@Schema(description = "The data held in NOMIS about a financial transaction")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderTransactionDto(
  @Schema(description = "The transaction id", example = "12345678")
  val transactionId: Long,

  @Schema(description = "The sequence number", example = "3")
  val transactionEntrySequence: Int,

  @Schema(description = "The offender id", example = "1234567")
  val offenderId: Long,

  @Schema(description = "The prisoner number or noms id", example = "A1234AA")
  val offenderNo: String,

  @Schema(description = "The booking id", example = "12345678")
  val bookingId: Long?,

  @Schema(description = "The caseload", example = "MDI")
  val caseloadId: String,

  @Schema(description = "The transaction amount", example = "2.14")
  val amount: BigDecimal,

  @Schema(description = "The transaction type defined in the TRANSACTION_TYPES table", example = "CANT")
  val type: String,

  @Schema(description = "Whether credit or debit")
  val postingType: PostingType,

  @Schema(description = "A description of the transaction entry", example = "Television")
  val description: String,

  @Schema(description = "The transaction date", example = "2025-06-14")
  val entryDate: LocalDate,

  @Schema(description = "The client unique reference", example = "MKI6431RETAILRECEIPTPHONE")
  val clientReference: String?,

  @Schema(description = "The transaction reference", example = "1423558449")
  val reference: String?,

  @Schema(description = "The account type")
  val subAccountType: SubAccountType,

  @Schema(description = "Dependent GL transaction entries")
  val generalLedgerTransactions: List<GeneralLedgerTransactionDto>,

  val createdAt: LocalDateTime,
  val createdBy: String,
  val createdByDisplayName: String,

  val lastModifiedAt: LocalDateTime? = null,
  val lastModifiedBy: String? = null,
  val lastModifiedByDisplayName: String? = null,
)

@Schema(description = "The data held in NOMIS about a general ledger transaction")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeneralLedgerTransactionDto(
  @Schema(description = "The transaction id", example = "12345678")
  val transactionId: Long,

  @Schema(description = "The sequence number", example = "3")
  val transactionEntrySequence: Int,

  @Schema(description = "The GL entry number", example = "2")
  val generalLedgerEntrySequence: Int,

  @Schema(description = "The caseload", example = "MDI")
  val caseloadId: String,

  @Schema(description = "The transaction amount", example = "2.14")
  val amount: BigDecimal,

  @Schema(description = "The transaction type defined in the TRANSACTION_TYPES table", example = "CANT")
  val type: String,

  @Schema(description = "Whether credit or debit")
  val postingType: PostingType,

  @Schema(description = "The account code", example = "21020")
  val accountCode: Int,

  @Schema(description = "A description of the transaction entry", example = "???")
  val description: String,

  @Schema(description = "When the transaction occurred", example = "2025-07-14T12:13:14")
  val transactionTimestamp: LocalDateTime,

  @Schema(description = "The transaction reference", example = "1423558449")
  val reference: String?,

  val createdAt: LocalDateTime,
  val createdBy: String,
  val createdByDisplayName: String,

  val lastModifiedAt: LocalDateTime? = null,
  val lastModifiedBy: String? = null,
  val lastModifiedByDisplayName: String? = null,
)
