package uk.gov.justice.digital.hmpps.nomisprisonerapi.bedassignments

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
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
class BedAssignmentHistoryResource(private val bedAssignmentHistoryService: BedAssignmentHistoryService) {
  @GetMapping("/prisoners/booking-id/{bookingId}/bed-assignments/{sequence}")
  @Operation(
    summary = "Get a bed assignment history record for a booking",
    description = "Retrieves a single bed assignment, for synchronisation. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = BedAssignmentHistoryResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Bed assignment or booking does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getBedAssignment(
    @Schema(description = "Booking Id", example = "2345678") @PathVariable bookingId: Long,
    @Schema(description = "Sequence within booking", example = "3") @PathVariable sequence: Int,
  ): BedAssignmentHistoryResponse = bedAssignmentHistoryService.getBedAssignment(bookingId, sequence)

  @GetMapping("/prisoners/{offenderNo}/bed-assignments")
  @Operation(
    summary = "Gets all bed assignment history for a prisoner",
    description = "Retrieves the bed assignment history across all of a prisoner's bookings, for migration and synchronisation. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PrisonerBedAssignmentHistoryResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getBedAssignmentsForPrisoner(
    @Schema(description = "Offender No AKA prisoner number", example = "A3745XD")
    @PathVariable
    offenderNo: String,
  ): PrisonerBedAssignmentHistoryResponse = bedAssignmentHistoryService.getBedAssignments(offenderNo)
}
