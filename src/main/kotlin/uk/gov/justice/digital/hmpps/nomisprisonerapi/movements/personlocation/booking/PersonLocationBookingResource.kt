package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.booking

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

@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@RestController
@Validated
@RequestMapping("/movements/{offenderNo}/person-locations/booking", produces = [MediaType.APPLICATION_JSON_VALUE])
class PersonLocationBookingResource(
  private val service: PersonLocationBookingService,
) {
  @GetMapping("/{bookingId}")
  @Operation(
    summary = "Get a single booking for an offender",
    description = "Get a single booking, without its movements, for an offender by booking ID. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Booking returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender or booking not found, or the booking does not belong to the offender",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getPersonLocationBooking(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Booking ID", example = "123") @PathVariable bookingId: Long,
  ): PersonLocationBooking = service.getPersonLocationBooking(offenderNo, bookingId)
}
