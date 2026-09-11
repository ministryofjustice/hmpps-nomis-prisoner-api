package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@RestController
@Validated
@RequestMapping("/movements/{offenderNo}/court/schedule", produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtScheduleResource(
  private val service: CourtScheduleService,
) {

  @GetMapping("/out/{eventId}")
  @Operation(
    summary = "Get a specific court schedule out for an offender",
    description = "Get a specific court schedule out for an offender by event ID. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender court schedule out returned",
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
        description = "Offender or court schedule in not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getCourtScheduleOut(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Event ID", example = "123") @PathVariable eventId: Long,
  ) = service.getCourtScheduleOut(offenderNo, eventId)

  @PutMapping("/out")
  @Operation(
    summary = "Inserts or updates a court schedule out for an offender, and potentially its return",
    description = "Creates or updates a court schedule out on the prisoner's latest booking, and potentially its return. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Court schedule out created or updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "One or more fields in the request contains invalid data",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun upsertCourtScheduleOut(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: UpsertCourtScheduleOut,
  ): UpsertCourtScheduleOutResponse = service.upsertCourtScheduleOut(offenderNo, request)

  @DeleteMapping("/out/{eventId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deleete a specific court schedule out for an offender",
    description = "Delete a specific court schedule out for an offender by event ID. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Offender court schedule out returned",
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
        responseCode = "409",
        description = "Unalbe to delete court schedule",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun deleteCourtScheduleOut(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Event ID", example = "123") @PathVariable eventId: Long,
  ) = service.deleteCourtScheduleOut(offenderNo, eventId)
}
