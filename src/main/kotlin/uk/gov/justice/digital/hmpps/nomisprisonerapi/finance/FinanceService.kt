package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransactionRepository
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional
class FinanceService(
  val repository: OffenderTransactionRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getTransaction(transactionId: Long, transactionEntrySequence: Int): TransactionDto = repository
    .findByIdOrNull(OffenderTransaction.Companion.Pk(transactionId, transactionEntrySequence))
    ?.let {
      TransactionDto(
        transactionId = it.transactionId,
        transactionEntrySequence = it.transactionEntrySequence,
        amount = it.entryAmount,
        type = it.transactionType.type,
        postingType = it.postingType.name,
        offenderNo = it.offenderBooking?.offender?.nomsId,
        entryDate = it.entryDate,
        reference = it.clientUniqueRef,
        subAccountType = it.subAccountType.name,
        // TODO description = null,
        holdBalance = it.trustAccount.holdBalance,
        currentBalance = it.trustAccount.currentBalance,
      )
    }
    ?: throw NotFoundException("Transaction with transactionId $transactionId and sequence $transactionEntrySequence not found")
}

@Schema(description = "The data held in NOMIS about a financial transaction")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TransactionDto(
  @Schema(description = "The transaction id", example = "12345678")
  val transactionId: Long,
  @Schema(description = "The sequence number", example = "3")
  val transactionEntrySequence: Int,
  @Schema(description = "The prisoner number or noms id", example = "A1234AA")
  val offenderNo: String?,
  @Schema(description = "The transaction amount", example = "2.14")
  val amount: BigDecimal,
  @Schema(description = "The transaction type defined in the TRANSACTION_TYPES table", example = "CANT")
  val type: String,
  @Schema(description = "Whether credit or debit", allowableValues = ["CR", "DR"])
  val postingType: String,
  @Schema(description = "The transaction date", example = "2025-06-14")
  val entryDate: LocalDate,
  @Schema(description = "The client unique reference", example = "MKI6431RETAILRECEIPTPHONE")
  val reference: String?,
  @Schema(description = "The account type", allowableValues = ["REG", "SAV", "SPND"])
  val subAccountType: String,
  @Schema(description = "The hold balance", example = "3.28")
  val holdBalance: BigDecimal?,
  @Schema(description = "The current balance", example = "4.67")
  val currentBalance: BigDecimal?,
)
