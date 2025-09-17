package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping("/finance/prison", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class PrisonBalanceResource(
  private val prisonBalanceService: PrisonBalanceService,
) {
  @GetMapping("/ids")
  @Operation(
    summary = "Gets all the caseloads (prisons) that have balance entries.",
    description = """Retrieves a list of caseloads that have a balance.

      This will include caseloads for prisons that have closed (e.g. Medway STC, Everthorpe) and also caseloads that 
      aren't prisons (e.g. Transfer, Ghost Holding Establishment).

      Certain caseloads (e.g. CADM_I, NWEB and LIC) don't have a balance so aren't returned.

      Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW role.""",
    responses = [
      ApiResponse(responseCode = "200", description = "Balance information returned."),
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
        description = "Prison does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonIds(): List<String> = prisonBalanceService.findAllIds()

  @GetMapping("/{prisonId}/balance")
  @Operation(
    summary = "Gets the balances for the prison.",
    description = "Retrieves all the balances for a prison. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW role.",
    responses = [
      ApiResponse(responseCode = "200", description = "Balance information returned."),
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
        description = "Prison does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonBalance(
    @Schema(description = "prisonId", example = "MDI")
    @PathVariable
    prisonId: String,
  ): PrisonBalanceDto = prisonBalanceService.getPrisonBalance(prisonId)
}

data class PrisonBalanceDto(
  @Schema(description = "The prison id", example = "MDI")
  val prisonId: String,
  @Schema(description = "The list of account balances for the prison")
  val accountBalances: List<PrisonAccountBalanceDto>,
)

data class PrisonAccountBalanceDto(
  @Schema(description = "The account code for the balance entry", example = "2101")
  val accountCode: Long,
  @Schema(description = "The balance for this prison and account code", example = "33.12")
  val balance: BigDecimal,
  @Schema(description = "The date and time of the last transaction OR the date and time of the consolidation of the transactions")
  val transactionDate: LocalDateTime,
)
