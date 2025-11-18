package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class MovementsResource(
  private val movementsService: MovementsService,
) {
  @GetMapping("/movements/{offenderNo}/temporary-absences")
  @Operation(
    summary = "Get temporary absence applications, schedules and external movements for an offender",
    description = "Get temporary absence applications, schedules and external movements for an offender. This is used to migrate temporary absences to DPS. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
        description = "Forbidden, requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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

  @GetMapping("/movements/{offenderNo}/temporary-absences/application/{applicationId}")
  @Operation(
    summary = "Get a specific temporary absence application for an offender",
    description = "Get a specific temporary absence application for an offender. Note that this does not include any children such as the scheduled or actual movements. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
        description = "Forbidden, requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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

  @PutMapping("/movements/{offenderNo}/temporary-absences/application")
  @Operation(
    summary = "Inserts or updates a temporary absence application for an offender",
    description = "Creates or updates a temporary absence application on the prisoner's latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Temporary absence application created",
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
  fun upsertTemporaryAbsenceApplication(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: UpsertTemporaryAbsenceApplicationRequest,
  ): UpsertTemporaryAbsenceApplicationResponse = movementsService.upsertTemporaryAbsenceApplication(offenderNo, request)

  @GetMapping("/movements/{offenderNo}/temporary-absences/scheduled-temporary-absence/{eventId}")
  @Operation(
    summary = "Get a specific scheduled temporary absence for an offender",
    description = "Get a specific scheduled temporary absence for an offender by event ID. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
        description = "Forbidden, requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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

  @GetMapping("/movements/{offenderNo}/temporary-absences/scheduled-temporary-absence-return/{eventId}")
  @Operation(
    summary = "Get a specific scheduled temporary absence return for an offender",
    description = "Get a specific scheduled temporary absence return for an offender by event ID. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
        description = "Forbidden, requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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

  @PutMapping("/movements/{offenderNo}/temporary-absences/scheduled-temporary-absence")
  @Operation(
    summary = "Inserts or updates a scheduled temporary absence for an offender, and potentially its return",
    description = "Creates or updates a scheduled temporary absence on the prisoner's latest booking, and potentially its return. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Scheduled temporary absence created or updated",
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
  fun upsertScheduledTemporaryAbsence(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: UpsertScheduledTemporaryAbsenceRequest,
  ): UpsertScheduledTemporaryAbsenceResponse = movementsService.upsertScheduledTemporaryAbsence(offenderNo, request)

  @GetMapping("/movements/{offenderNo}/temporary-absences/outside-movement/{appMultiId}")
  @Operation(
    summary = "Get a specific temporary absence application outside movement for an offender",
    description = "Get a specific temporary absence application outside movement for an offender by application multi ID. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender temporary absence application outside movement returned",
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
        description = "Offender or outside movement not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getTemporaryAbsenceApplicationOutsideMovement(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Application Multi ID", example = "123") @PathVariable appMultiId: Long,
  ) = movementsService.getTemporaryAbsenceApplicationOutsideMovement(offenderNo, appMultiId)

  @PostMapping("/movements/{offenderNo}/temporary-absences/outside-movement")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Inserts a temporary absence outside movement for an offender",
    description = "Creates a temporary absence outside movement on the prisoner's latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Temporary absence outside movement created",
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
  fun createTemporaryAbsenceOutsideMovement(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: CreateTemporaryAbsenceOutsideMovementRequest,
  ): CreateTemporaryAbsenceOutsideMovementResponse = movementsService.createTemporaryAbsenceOutsideMovement(offenderNo, request)

  @GetMapping("/movements/{offenderNo}/temporary-absences/temporary-absence/{bookingId}/{movementSeq}")
  @Operation(
    summary = "Get a specific temporary absence for an offender",
    description = "Get a specific temporary absence for an offender by booking ID and movement sequence. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender temporary absence returned",
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
        description = "Offender or temporary absence not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getTemporaryAbsence(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Booking ID", example = "123") @PathVariable bookingId: Long,
    @Schema(description = "Movement Sequence", example = "1") @PathVariable movementSeq: Int,
  ) = movementsService.getTemporaryAbsence(offenderNo, bookingId, movementSeq)

  @PostMapping("/movements/{offenderNo}/temporary-absences/temporary-absence")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Inserts a temporary absence for an offender",
    description = "Creates a temporary absence on the prisoner's latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Temporary absence created",
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
  fun createTemporaryAbsence(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: CreateTemporaryAbsenceRequest,
  ): CreateTemporaryAbsenceResponse = movementsService.createTemporaryAbsence(offenderNo, request)

  @GetMapping("/movements/{offenderNo}/temporary-absences/temporary-absence-return/{bookingId}/{movementSeq}")
  @Operation(
    summary = "Get a specific temporary absence return for an offender",
    description = "Get a specific temporary absence return for an offender by booking ID and movement sequence. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender temporary absence return returned",
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
        description = "Offender or temporary absence return not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getTemporaryAbsenceReturn(
    @Schema(description = "Offender number (NOMS ID)", example = "A1234BC") @PathVariable offenderNo: String,
    @Schema(description = "Booking ID", example = "123") @PathVariable bookingId: Long,
    @Schema(description = "Movement Sequence", example = "1") @PathVariable movementSeq: Int,
  ) = movementsService.getTemporaryAbsenceReturn(offenderNo, bookingId, movementSeq)

  @PostMapping("/movements/{offenderNo}/temporary-absences/temporary-absence-return")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Inserts a temporary absence return for an offender",
    description = "Creates a temporary absence return on the prisoner's latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Temporary absence return created",
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
  fun createTemporaryAbsenceReturn(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: CreateTemporaryAbsenceReturnRequest,
  ): CreateTemporaryAbsenceReturnResponse = movementsService.createTemporaryAbsenceReturn(offenderNo, request)
}
