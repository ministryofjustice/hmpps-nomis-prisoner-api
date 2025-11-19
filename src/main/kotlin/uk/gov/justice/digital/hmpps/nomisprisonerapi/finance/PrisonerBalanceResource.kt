package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
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
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/finance/prisoners"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class PrisonerBalanceResource(
  private val prisonerBalanceService: PrisonerBalanceService,
) {
  @GetMapping("/ids")
  @Operation(
    summary = "Gets the rootOffenderIds for all prisoners with a non-negative trust account balance",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(responseCode = "200", description = "paged list of prisoner ids"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrisonerBalanceIdentifiers(
    @PageableDefault(sort = ["offenderId"], direction = Sort.Direction.ASC)
    @ParameterObject
    pageRequest: Pageable,
    @Schema(description = "Prison id") @RequestParam(name = "prisonId") prisonIds: List<String>?,
  ): PagedModel<Long> = prisonerBalanceService.findAllPrisonersWithAccountBalance(prisonIds, pageRequest)

  @GetMapping("/ids/all-from-id")
  @Operation(
    summary = "Gets the rootOffenderIds for all prisoners with a non-negative trust account balance",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(responseCode = "200", description = "paged list of prisoner ids"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrisonerBalanceIdentifiersFromId(
    @Schema(description = "If supplied get offenders starting after this id", required = false, example = "1555999")
    @RequestParam(value = "rootOffenderId", defaultValue = "0")
    rootOffenderId: Long,
    @Schema(description = "Number of offenders to get", required = false, defaultValue = "10")
    @RequestParam(value = "pageSize", defaultValue = "10")
    pageSize: Int,
    @Schema(description = "Prison id") @RequestParam(name = "prisonId") prisonIds: List<String>?,
  ): RootOffenderIdsWithLast = prisonerBalanceService.findAllPrisonersWithAccountBalanceFromId(rootOffenderId, pageSize, prisonIds)

  data class RootOffenderIdsWithLast(
    val rootOffenderIds: List<Long>,
    val lastOffenderId: Long,
  )

  @GetMapping("/{rootOffenderId}/balance")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner's finance details by their root offender id",
    description = "Retrieves a prisoner's trust account details for all caseloads with a balance. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Transaction Information Returned"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonerAccountDetails(
    @Schema(description = "rootOffenderId", example = "123456")
    @PathVariable
    rootOffenderId: Long,
  ): PrisonerBalanceDto = prisonerBalanceService.getPrisonerAccounts(rootOffenderId)
}

@Schema(description = "Finance details for a prisoner")
data class PrisonerBalanceDto(
  @Schema(description = "The root offender Id", example = "12345")
  val rootOffenderId: Long,

  @Schema(description = "The prison Number", example = "A1234BC")
  val prisonNumber: String,

  @Schema(description = "The accounts associated with the prisoner")
  val accounts: List<PrisonerAccountDto>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerAccountDto(
  @Schema(description = "The id of the prison", example = "MDI")
  val prisonId: String,

  @Schema(description = "The id of the last transaction associated with the balance", example = "123")
  val lastTransactionId: Long,

  @Schema(description = "The date and time of the last transaction")
  val transactionDate: LocalDateTime,

  @Schema(description = "The account code for the balance entry", example = "2101")
  val accountCode: Long,

  @Schema(description = "The account balance", example = "12.50")
  val balance: BigDecimal,

  @Schema(description = "The amount on hold", example = "12.50")
  val holdBalance: BigDecimal? = null,
)
