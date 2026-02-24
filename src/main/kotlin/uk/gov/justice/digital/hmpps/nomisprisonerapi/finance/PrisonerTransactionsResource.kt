package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PostingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SubAccountType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class PrisonerTransactionsResource(
  private val transactionsService: TransactionsService,
) {
  @GetMapping("/transactions/{transactionId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner transaction group by id",
    description = "Retrieves prisoner transactions (all in sequence) identified by id. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction Information Returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Transaction does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getTransaction(
    @Schema(description = "Id", example = "123456789")
    @PathVariable
    transactionId: Long,
  ): List<OffenderTransactionDto> = transactionsService.getOffenderTransactions(transactionId)

  @GetMapping("/transactions/from/{transactionId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a page of prisoner transaction Ids starting from an id onwards",
    description = """Retrieves transactions to be iterated over using last transaction Id and entry date.
      Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction Information Returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Transaction does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun findPrisonerTransactionsFromId(
    @Schema(description = "If supplied get prisoner transaction starting after this id", example = "1555999")
    @PathVariable(value = "transactionId")
    transactionId: Long,
    @Schema(description = "Number of prisoner transaction ids to get", required = false, defaultValue = "20")
    @RequestParam(value = "size", defaultValue = "20")
    size: Int,
    @RequestParam(value = "entryDate")
    @Parameter(description = "Filter results by transactions that were created on the given date", required = false, example = "2024-11-03")
    entryDate: LocalDate = LocalDate.now(),
  ): PrisonerTransactionIdsPage = transactionsService.getPrisonerTransactionIds(
    transactionId = transactionId,
    pageSize = size,
    entryDate = entryDate,
  )

  @GetMapping("/transactions/from/{transactionId}/{transactionEntrySequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get a page of orphan (prison transactions with no offender) GL transactions starting from an id,seq,glseq onwards",
    description = "Retrieves transactions to be iterated over. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction Information Returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Transaction does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun findOffenderTransactionsFromIdAndSequence(
    @Schema(description = "Id of last transaction before intended start", example = "123456789")
    @PathVariable
    transactionId: Long,
    @Schema(description = "Sequence of last transaction before intended start", example = "3")
    @PathVariable
    transactionEntrySequence: Int,
    @Schema(
      description = """Number of Offender transaction records to get. The first record returned will be the first one *after*
        the given id/seq combination.""",
      example = "500",
      required = false,
      defaultValue = "100",
    )
    @RequestParam(required = false, defaultValue = "100")
    pageSize: Int,
  ): List<OffenderTransactionDto> = transactionsService
    .findOffenderTransactionsFromId(
      transactionId,
      transactionEntrySequence,
      pageSize,
    )
}

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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerTransactionIdsPage(
  @Schema(description = "Page of prisoner Transaction Ids")
  val ids: List<PrisonerTransactionIdResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prisoner transaction id")
data class PrisonerTransactionIdResponse(
  @Schema(description = "The prisoner transaction id", required = true)
  val transactionId: Long,
)
