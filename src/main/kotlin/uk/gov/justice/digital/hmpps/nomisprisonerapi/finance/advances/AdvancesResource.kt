package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.advances

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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjustmentReason
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/finance"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class AdvancesResource(
  private val service: AdvancesService,
) {
  @GetMapping("/advances/{advanceId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get an advance by id",
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
    @Schema(description = "Advance Id aka offender payment profile id", example = "123456789")
    @PathVariable
    advanceId: Long,
  ): PrisonerAdvanceDto = service.getAdvance(advanceId)

  @GetMapping("/advances/active-count")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a count of all active prisoner advances",
    description = "Retrieves a count of all prisoner advances that are ACTIVE. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getActiveAdvancesCount(): AdvancesCount = service.getActiveAdvancesCount()

  @GetMapping("/prisoners/{prisonNumber}/advances")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner's advances by their prison number",
    description = "Retrieves a prisoner's advances by prison number. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
    @Schema(description = "Prison number aka noms id / offender id display", example = "A1234BC")
    @PathVariable
    prisonNumber: String,
    @Schema(description = "whether to return just active advances", example = "true", defaultValue = "false")
    @RequestParam
    activeOnly: Boolean = false,
  ): List<PrisonerAdvanceDto> = service.getAdvances(prisonNumber, activeOnly)

  @GetMapping("/prisoners/root-offender-id/{rootOffenderId}/advances")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner's advances by their root offender id",
    description = "Retrieves a prisoner's advances by root offender id. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getPrisonerAdvancesById(
    @Schema(description = "The root offender id", example = "123456")
    @PathVariable
    rootOffenderId: Long,
    @Schema(description = "Whether to return just active advances", example = "true", defaultValue = "false")
    @RequestParam
    activeOnly: Boolean = false,
  ): List<PrisonerAdvanceDto> = service.getAdvances(rootOffenderId, activeOnly)

  @GetMapping("/advances/deduction-id/{deductionId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner advance by offender deduction id",
    description = "Retrieves a prisoner advance identified by an associated offender deduction id. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getAdvanceByDeductionId(
    @Schema(description = "Offender deduction Id", example = "123456789")
    @PathVariable
    deductionId: Long,
  ): PrisonerAdvanceDto = service.getAdvanceByDeductionId(deductionId)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerAdvanceDto(
  @Schema(description = "The advance id", example = "123456")
  val id: Long,
  @Schema(description = "The prisonNumber", example = "A1234BC")
  val prisonNumber: String,
  @Schema(description = "The caseload", example = "MDI")
  val caseloadId: String,

  @Schema(description = "The total amount in pence to be paid to the prisoner, so £1.50 returned as 150", example = "150")
  val advanceAmount: Long,
  @Schema(description = "The date of the advance given to the prisoner", example = "2024-06-01")
  val advanceDate: LocalDate?,
  @Schema(description = "The amount the prisoner still owes, in pence", example = "125")
  val outstandingBalance: Long,

  @Schema(description = "The amount to be taken from the prisoner as a weekly repayment, in pence", example = "150")
  val repaymentAmount: Long,
  @Schema(description = "The start date of the repayment", example = "2024-06-01")
  val startDate: LocalDate,

  @Schema(description = "The reference for the advance", example = "REF12345")
  val reference: String?,
  @Schema(description = "The comment for the advance", example = "Advance for personal expenses")
  val comment: String?,

  @Schema(description = "The information number", example = "12345678-1")
  val informationNumber: String,

  @Schema(description = "How much has been paid off so far, in pence", example = "1130")
  val deductionAmount: Long? = null,

  @Schema(description = "The reason code for the adjustment", enumAsRef = true)
  val adjustmentReason: AdjustmentReason? = null,
  @Schema(description = "How much has been written off so far, in pence", example = "1020")
  val adjustmentAmount: Long? = null,

  @Schema(description = "The user who created the advance", example = "FRED_SMITH")
  val createdBy: String,
  @Schema(description = "The date time when the advance was created", example = "2024-06-01T12:00:00")
  val createDatetime: LocalDateTime,

  @Schema(description = "The status of the advance", example = "ACTIVE")
  val status: AdvanceStatus,

  @Schema(description = "The transaction id for the advance creation", example = "123456789")
  val advanceCreateTransactionId: Long,
  @Schema(description = "The date time of the last transaction for the advance", example = "2025-07-14T12:13:14")
  val lastTransactionTimestamp: LocalDateTime,
)

data class AdvancesCount(
  @Schema(description = "The count of active advances", example = "3")
  val activeCount: Long,
)

enum class AdvanceStatus { ACTIVE, REPAID, WRITTEN_OFF }
