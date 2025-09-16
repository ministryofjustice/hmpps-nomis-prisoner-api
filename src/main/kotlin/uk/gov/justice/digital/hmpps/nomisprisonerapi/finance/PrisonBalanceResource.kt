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

@RestController
@Validated
@RequestMapping("/finance", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('NOMIS_FINANCE')")
class PrisonBalanceResource {
  @GetMapping("/prison/{prisonId}/balance")
  @Operation(
    summary = "Gets the balances for the prison.",
    description = "Retrieves all the balances for a prison. Requires NOMIS_FINANCE role.",
    responses = [
      ApiResponse(responseCode = "200", description = "Balance information returned."),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires NOMIS_FINANCE",
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
  ): PrisonBalanceDto = PrisonBalanceDto(
    prisonId = prisonId,
    accountBalances = listOf(
      PrisonAccountBalanceDto(
        accountCode = 2101,
        accountPeriodId = 202604,
        balance = BigDecimal("33.12"),
      ),
    ),
  )
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
  @Schema(description = "The most recent accounting period that this entry relates to", example = "202604")
  val accountPeriodId: Long,
  @Schema(description = "The balance for this prison and account code", example = "33.12")
  val balance: BigDecimal,
)
