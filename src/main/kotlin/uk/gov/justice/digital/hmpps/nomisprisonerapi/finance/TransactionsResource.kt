package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import io.swagger.v3.oas.annotations.Operation
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
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('NOMIS_TRANSACTIONS')")
class TransactionsResource(
  private val transactionsService: TransactionsService,
) {
  @GetMapping("/transactions/{transactionId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get a transaction group by id",
    description = "Retrieves transactions (all in sequence) identified by id. Requires NOMIS_TRANSACTIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction Information Returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires NOMIS_TRANSACTIONS",
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

  @GetMapping("/transactions/{transactionId}/general-ledger")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get a transaction by id and sequence number",
    description = "Retrieves a prisoner transaction. Requires NOMIS_TRANSACTIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction Information Returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires NOMIS_TRANSACTIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Transaction does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getGLTransaction(
    @Schema(description = "Id", example = "123456789")
    @PathVariable
    transactionId: Long,
  ): List<GeneralLedgerTransactionDto> = transactionsService.getGeneralLedgerTransactions(transactionId)

  @GetMapping("/transactions/{date}/first")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get the first transaction on the given date",
    description = "Intended to be used to start retrieval of transactions from this date. Requires NOMIS_TRANSACTIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction Information Returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires NOMIS_TRANSACTIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Transaction does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getFirstTransactionOn(
    @Schema(description = "Starting date", example = "2025-08-11")
    @PathVariable
    date: LocalDate,
  ): Long = transactionsService.getFirstTransactionIdOn(date)

  @GetMapping("/transactions/from/{transactionId}/{transactionEntrySequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get a page of orphan GL transactions starting from an id,seq,glseq onwards",
    description = "Retrieves transactions to be iterated over. Requires NOMIS_TRANSACTIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction Information Returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires NOMIS_TRANSACTIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Transaction does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun findOffenderTransactionsFromId(
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

enum class SourceSystem { DPS, NOMIS }
