package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.scheduledpayments

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(value = ["/finance"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "401",
      description = "Unauthorized to access this endpoint",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "403",
      description = "Missing required role. Requires the ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW role",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  ],
)
class ScheduledPaymentsResource(
  private val service: ScheduledPaymentsService,
) {
  @GetMapping("/scheduled-payments/{scheduledPaymentId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a scheduled payment by id",
    description = "Retrieves a prisoner scheduled payment identified by id. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled Payment Information Returned"),
      ApiResponse(
        responseCode = "404",
        description = "Scheduled Payment does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getScheduledPayment(
    @Schema(description = "Scheduled Payment Id aka offender payment profile id", example = "123456789")
    @PathVariable
    scheduledPaymentId: Long,
  ): ScheduledPaymentDto = service.getScheduledPayment(scheduledPaymentId)

  @GetMapping("/scheduled-payments/active-count")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a count of all active prisoner scheduled payments",
    description = "Retrieves a count of all prisoner scheduled payments that are ACTIVE. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled Payment Information Returned"),
      ApiResponse(
        responseCode = "404",
        description = "Scheduled Payment does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getActiveScheduledPaymentsCount(): ScheduledPaymentsCount = service.getActiveScheduledPaymentsCount()

  @GetMapping("/prisoners/{prisonNumber}/scheduled-payments")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner's scheduled payments by their prison number",
    description = "Retrieves a prisoner's scheduled payments. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled Payment Information Returned"),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonerScheduledPayments(
    @Schema(description = "Prison number aka noms id / offender id display", example = "A1234BC")
    @PathVariable
    prisonNumber: String,
    @Schema(description = "whether to return just active advances", example = "true", defaultValue = "false")
    @RequestParam
    activeOnly: Boolean = false,
  ): List<ScheduledPaymentDto> = service.getScheduledPayments(prisonNumber, activeOnly)

  @GetMapping("/prisoners/root-offender-id/{rootOffenderId}/scheduled-payments")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a prisoner's scheduled payments by their root offender id",
    description = "Retrieves a prisoner's scheduled payments by root offender id. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "Scheduled Payment Information Returned"),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPrisonerScheduledPaymentsById(
    @Schema(description = "The root offender id", example = "A1234BC")
    @PathVariable
    rootOffenderId: Long,
    @Schema(description = "whether to return just active advances", example = "true", defaultValue = "false")
    @RequestParam
    activeOnly: Boolean = false,
  ): List<ScheduledPaymentDto> = service.getScheduledPayments(rootOffenderId, activeOnly)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ScheduledPaymentDto(
  @Schema(description = "The scheduled payment id", example = "123456")
  val id: Long,

  @Schema(description = "The prisonNumber", example = "A1234BC")
  val prisonNumber: String,

  @Schema(description = "The caseload", example = "MDI")
  val caseloadId: String,

  @Schema(description = "transaction type", example = "TELE")
  val transactionType: String,

  @Schema(description = "The sub account code the payment will be taken from", example = "2101")
  val subAccountCode: Long,
  @Schema(description = "The payment amount in pence, to be paid each time, to the prison, so £1.50 returned as 150", example = "150")
  val amount: Long,
  @Schema(description = "Day of the week the scheduled payment is paid", example = "MON")
  val dayOfWeek: WeekDay,

  @Schema(description = "The start date of the scheduled payment", example = "2024-06-01")
  val startDate: LocalDate,
  @Schema(description = "The date the scheduled payment ends or null if it does not end", example = "2024-06-01")
  val endDate: LocalDate?,

  @Schema(description = "The reference for the scheduled payment", example = "REF12345")
  val reference: String?,
  @Schema(description = "The comment for the scheduled payment", example = "Sched payment for personal expenses")
  val comment: String?,
  @Schema(description = "The user who created the scheduled payment", example = "FRED_SMITH")
  val createdBy: String,
)

data class ScheduledPaymentsCount(
  @Schema(description = "The count of active scheduled payments", example = "3")
  val activeCount: Long,
)
