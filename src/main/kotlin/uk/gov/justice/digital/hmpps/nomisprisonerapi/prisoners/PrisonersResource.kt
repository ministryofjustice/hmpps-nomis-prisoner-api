package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.constraints.Pattern
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visits.OFFENDER_NO_PATTERN
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonersResource(private val prisonerService: PrisonerService) {
  @PreAuthorize("hasRole('ROLE_SYNCHRONISATION_REPORTING')")
  @GetMapping("/prisoners/ids")
  @Operation(
    summary = "Gets the identifiers for all prisoners. Currently only active prisoners are supported",
    description = "Requires role SYNCHRONISATION_REPORTING.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "paged list of prisoner ids",
      ),
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
        description = "Forbidden to access this endpoint when role SYNCHRONISATION_REPORTING not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrisonerIdentifiers(
    @PageableDefault(sort = ["bookingId"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "active", required = false, defaultValue = "true")
    @Parameter(
      description = "Only return active prisoners currently in prison",
    )
    active: Boolean = false,
  ): Page<PrisonerId> = if (active) prisonerService.findAllActivePrisoners(pageRequest) else throw UnsupportedOperationException("Not implemented - only active prisoners can be found")

  @PreAuthorize("hasRole('ROLE_SYNCHRONISATION_REPORTING')")
  @PostMapping("/prisoners/bookings")
  @Operation(
    summary = "Gets prisoner details for a list of bookings",
    description = "Requires role SYNCHRONISATION_REPORTING.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "list of prisoner details",
      ),
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
        description = "Forbidden to access this endpoint when role SYNCHRONISATION_REPORTING not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrisonerBookings(
    @Schema(description = "A list of prisoner details")
    @RequestBody
    bookingIds: List<Long>,
  ): List<PrisonerDetails> = prisonerService.findPrisonerDetails(bookingIds)

  @PreAuthorize("hasRole('ROLE_SYNCHRONISATION_REPORTING')")
  @GetMapping("/prisoners/{offenderNo}/merges")
  @Operation(
    summary = "Gets prisoner's list of merge details since a given date",
    description = "Requires role SYNCHRONISATION_REPORTING.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "list of prisoner merges",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = MergeDetail::class)),
          ),
        ],
      ),
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
        description = "Forbidden to access this endpoint when role SYNCHRONISATION_REPORTING not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrisonerMerges(
    @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
    @PathVariable
    @Pattern(regexp = OFFENDER_NO_PATTERN)
    offenderNo: String,
    @RequestParam(value = "fromDate", required = false)
    @Parameter(
      description = "The earliest date to search for merges from",
    )
    fromDate: LocalDate?,
  ): List<MergeDetail> = prisonerService.findPrisonerMerges(offenderNo, fromDate)
}

data class PrisonerId(
  val bookingId: Long,
  val offenderNo: String,
)

@Schema(description = "Details of a prisoner booking")
data class PrisonerDetails(
  @Schema(description = "The NOMIS reference", example = "A1234AA")
  val offenderNo: String,
  @Schema(description = "The NOMIS booking ID", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The prisoner's current location", example = "BXI, OUT")
  val location: String,
)

@Schema(description = "Details of a prisoner merge")
data class MergeDetail(
  @Schema(description = "The NOMIS reference of record was merged from", example = "A1234AA")
  val fromOffenderNo: String,
  @Schema(description = "The booking that was merged from", example = "12345678")
  val fromBookingId: Long,
  @Schema(description = "The NOMIS reference of record was merged to", example = "A1234AA")
  val toOffenderNo: String,
  @Schema(description = "The booking that was merged to", example = "12345678")
  val toBookingId: Long,
  @Schema(description = "When the merge happened", example = "2021-01-01T12:34:56")
  val dateTime: LocalDateTime,
)
