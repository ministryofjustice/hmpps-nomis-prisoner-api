package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

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

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISON_PERSON')")
class IdentifyingMarksResource(private val service: IdentifyingMarksService) {

  @GetMapping("/bookings/{bookingId}/identifying-marks")
  @Operation(
    summary = "Get identifying marks for a prisoner's booking",
    description = "Retrieves identifying marks for a booking. Requires ROLE_NOMIS_PRISON_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Identifying marks Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = BookingIdentifyingMarksResponse::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISON_PERSON",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getBookingIdentifyingMarks(
    @Schema(description = "Booking id", example = "12345") @PathVariable bookingId: Long,
  ): BookingIdentifyingMarksResponse = service.getIdentifyingMarks(bookingId)
}
