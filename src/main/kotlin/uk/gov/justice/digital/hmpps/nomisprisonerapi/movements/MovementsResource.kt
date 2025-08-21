package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

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
class MovementsResource(
  private val movementsService: MovementsService,
) {
  @PreAuthorize("hasRole('ROLE_NOMIS_MOVEMENTS')")
  @GetMapping("/movements/{offenderNo}/temporary-absences")
  @Operation(
    summary = "Get temporary absence applications, schedules and external movements for an offender",
    description = "Get temporary absence applications, schedules and external movements for an offender. This is used to migrate temporary absences to DPS. Requires role NOMIS_MOVEMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender temporary absences returned",
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
        description = "Forbidden, requires role NOMIS_MOVEMENTS",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getTemporaryAbsencesAndMovements(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
  ): OffenderTemporaryAbsencesResponse = movementsService.getTemporaryAbsencesAndMovements(offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_MOVEMENTS')")
  @GetMapping("/movements/{offenderNo}/temporary-absences/application/{applicationId}")
  @Operation(
    summary = "Get a specific temporary absence application for an offender",
    description = "Get a specific temporary absence application for an offender. Note that this does not include any children such as the scheduled or actual movements. Requires role NOMIS_MOVEMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender temporary absence application returned",
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
        description = "Forbidden, requires role NOMIS_MOVEMENTS",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender or application not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getTemporaryAbsenceApplication(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Application ID", example = "123") @PathVariable applicationId: Long,
  ) = movementsService.getTemporaryAbsenceApplication(offenderNo, applicationId)

  @PreAuthorize("hasRole('ROLE_NOMIS_MOVEMENTS')")
  @GetMapping("/movements/{offenderNo}/temporary-absences/scheduled-temporary-absence/{eventId}")
  @Operation(
    summary = "Get a specific scheduled temporary absence for an offender",
    description = "Get a specific scheduled temporary absence for an offender by event ID. Requires role NOMIS_MOVEMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender scheduled temporary absence returned",
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
        description = "Forbidden, requires role NOMIS_MOVEMENTS",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender or scheduled temporary absence not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getScheduledTemporaryAbsence(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Event ID", example = "123") @PathVariable eventId: Long,
  ) = movementsService.getScheduledTemporaryAbsence(offenderNo, eventId)

  @PreAuthorize("hasRole('ROLE_NOMIS_MOVEMENTS')")
  @GetMapping("/movements/{offenderNo}/temporary-absences/scheduled-temporary-absence-return/{eventId}")
  @Operation(
    summary = "Get a specific scheduled temporary absence return for an offender",
    description = "Get a specific scheduled temporary absence return for an offender by event ID. Requires role NOMIS_MOVEMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender scheduled temporary absence return returned",
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
        description = "Forbidden, requires role NOMIS_MOVEMENTS",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender or scheduled temporary absence return not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getScheduledTemporaryAbsenceReturn(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Event ID", example = "123") @PathVariable eventId: Long,
  ) = movementsService.getScheduledTemporaryAbsenceReturn(offenderNo, eventId)
}
