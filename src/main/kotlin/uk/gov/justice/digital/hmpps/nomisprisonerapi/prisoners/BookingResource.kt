package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class BookingsResource(private val bookingService: BookingService) {
  @PreAuthorize("hasAnyRole('ROLE_SYNCHRONISATION_REPORTING')")
  @GetMapping("/bookings/ids/latest-from-id")
  @Operation(
    summary = "Gets the identifiers for all latest bookings.",
    description = """Gets the specified number of latest bookings starting after the given id number.
      Clients can iterate through all bookings by calling this endpoint using the id from the last call (omit for first call).
      Iteration ends when the returned prisonerIds list has size less than the requested page size.
      Requires role SYNCHRONISATION_REPORTING.""",
    responses = [
      ApiResponse(responseCode = "200", description = "list of prisoner ids containing bookingId and offenderNo"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role SYNCHRONISATION_REPORTING not present",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getAllLatestBookingsFromId(
    @Schema(description = "If supplied get offenders starting after this id", required = false, example = "1555999")
    @RequestParam(value = "bookingId", defaultValue = "0")
    bookingId: Long,
    @Schema(description = "If supplied only return bookings that are still active", required = false, example = "true")
    @RequestParam(value = "activeOnly", defaultValue = "false")
    activeOnly: Boolean,
    @Schema(description = "Number of bookings to get", required = false, defaultValue = "10")
    @RequestParam(value = "pageSize", defaultValue = "10")
    pageSize: Int,
  ): BookingIdsWithLast = bookingService.findAllLatestBookingFromId(lastBookingId = bookingId, activeOnly = activeOnly, pageSize = pageSize)
}

data class BookingIdsWithLast(
  val prisonerIds: List<PrisonerIds>,
  val lastBookingId: Long,
)
