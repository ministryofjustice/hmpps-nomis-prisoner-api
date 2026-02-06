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
@PreAuthorize("hasRole('NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonTransactionsResource(
  private val transactionsService: TransactionsService,
) {
  @GetMapping("/transactions/{transactionId}/general-ledger")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a transaction by id and sequence number",
    description = "Retrieves a prisoner transaction. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getGLTransaction(
    @Schema(description = "Id", example = "123456789")
    @PathVariable
    transactionId: Long,
  ): List<GeneralLedgerTransactionDto> = transactionsService.getGeneralLedgerTransactions(transactionId)

  @GetMapping("/transactions/{date}/first")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get the first general ledger transaction on the given date",
    description = """
      Intended to be used to start retrieval of transactions from this date. 
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
  fun getFirstTransactionOn(
    @Schema(description = "Starting date", example = "2025-08-11")
    @PathVariable
    date: LocalDate,
  ): Long = transactionsService.getFirstGeneralLedgerTransactionIdOn(date)

  @GetMapping("/transactions/from/{transactionId}/{transactionEntrySequence}/{generalLedgerEntrySequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get a transaction by id and sequence number",
    description = "Retrieves a prisoner transaction. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun findNonOffenderTransactionsFromId(
    @Schema(description = "Id of last transaction before intended start", example = "123456789")
    @PathVariable
    transactionId: Long,
    @Schema(description = "Sequence of last transaction before intended start", example = "3")
    @PathVariable
    transactionEntrySequence: Int,
    @Schema(description = "GL Sequence of last transaction before intended start", example = "1")
    @PathVariable
    generalLedgerEntrySequence: Int,
    @Schema(
      description = """Number of GL transaction records to get. The first record returned will be the first one *after*
        the given id/seq/glseq combination.
        
        Note that slightly fewer records than requested will usually 
        be returned as there are several records per transaction id (potentially over 2000), so the records of the 
        last id will be trimmed.
        The assumption is that they are an incomplete or truncated set for that transaction id""",
      example = "500",
      required = false,
      defaultValue = "100",
    )
    @RequestParam(required = false, defaultValue = "100")
    pageSize: Int,
  ): List<GeneralLedgerTransactionDto> = transactionsService
    .findOrphanTransactionsFromId(
      transactionId,
      transactionEntrySequence,
      generalLedgerEntrySequence,
      pageSize,
    )

  @GetMapping("/transactions/prison/{prisonId}/{date}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all  general ledger (only) transactions for a prison on the given date",
    description = """
      Get all prison transactions on this date. It will also return transactions related
      to the first transaction (from previous day) and end transaction of the day (from next day), to ensure full
      transactions (ie for all sequence values) are returned.
      Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction list Returned"),
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
  fun getTransactionsOn(
    @Schema(description = "prisonId", example = "MDI")
    @PathVariable
    prisonId: String,
    @Schema(description = "Starting date", example = "2025-08-11")
    @PathVariable
    date: LocalDate,
  ): List<GeneralLedgerTransactionDto> = transactionsService.getGeneralLedgerTransactionsForPrison(prisonId, date)
}
