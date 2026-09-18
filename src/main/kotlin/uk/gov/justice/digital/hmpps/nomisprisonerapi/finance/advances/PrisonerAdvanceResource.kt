package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import com.fasterxml.jackson.annotation.JsonInclude
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(value = ["/finance/prisoners"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class PrisonerAdvanceResource(
  private val service: PrisonerAdvanceService,
) {
  @GetMapping("/advances/{advanceId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner advance by id",
    description = "Retrieves a prisoner advance identified by id. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Advance Information Returned"),
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
        description = "Advance does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAdvance(
    @Schema(description = "Id", example = "123456789")
    @PathVariable
    advanceId: Long,
  ): PrisonerAdvanceDto = service.getAdvance(advanceId)

  @GetMapping("/{prisonNumber}/advances")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner's advances by their prison number",
    description = "Retrieves a prisoner's advances. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Advance Information Returned"),
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
  fun getPrisonerAdvances(
    @Schema(description = "prisonNumber", example = "A1234BC")
    @PathVariable
    prisonNumber: String,
  ): List<PrisonerAdvanceDto> = service.getAdvances(prisonNumber)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerAdvanceDto(
  @Schema(description = "The advance id", example = "123456")
  val id: Long,

  @Schema(description = "The prisonNumber", example = "A1234BC")
  val prisonNumber: String,

  @Schema(description = "The caseload", example = "MDI")
  val caseloadId: String,

  @Schema(description = "transaction type", example = "TELE")
  val transactionType: String,

  @Schema(description = "The total amount in pence to be paid to the prisoner, so £1.50 returned as 150", example = "150")
  val advanceAmount: Long,
  @Schema(description = "The date of the advance given to the prisoner", example = "2024-06-01")
  val advanceDate: LocalDate?,

  @Schema(description = "The amount to be taken from the prisoner as a weekly repayment, in pence", example = "150")
  val repaymentAmount: Long,
  @Schema(description = "The start date of the repayment", example = "2024-06-01")
  val startDate: LocalDate,

  @Schema(description = "The reference for the advance", example = "REF12345")
  val reference: String?,
  @Schema(description = "The comment for the advance", example = "Advance for personal expenses")
  val comment: String?,
  @Schema(description = "The user who created the advance", example = "FRED_SMITH")
  val createdBy: String,

  @Schema(description = "The status of the advance", example = "ACTIVE")
  val status: String?,
)
