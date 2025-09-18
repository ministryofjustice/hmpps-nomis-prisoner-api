package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class AlertsResource(
  private val alertsService: AlertsService,
  private val alertsReferenceDataService: AlertsReferenceDataService,
) {

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/booking-id/{bookingId}/alerts/{alertSequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get an alert by bookingId and alert sequence",
    description = "Retrieves an prisoner alert. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Alert Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AlertResponse::class),
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
        description = "Alert does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAlert(
    @Schema(description = "Booking Id", example = "12345")
    @PathVariable
    bookingId: Long,
    @Schema(description = "Alert sequence", example = "3")
    @PathVariable
    alertSequence: Long,
  ): AlertResponse = alertsService.getAlert(bookingId, alertSequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/alerts/to-migrate")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets alert for latest booking",
    description = "Retrieves alerts for a prisoner from latest all bookings. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Alerts Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerAlertsResponse::class),
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
        description = "Prisoner does not exist or has no bookings",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAlertsToMigrate(
    @Schema(description = "Offender No AKA prisoner number", example = "A1234AK")
    @PathVariable
    offenderNo: String,
  ): PrisonerAlertsResponse = alertsService.getAlerts(offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/alerts/reconciliation")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets active alerts for latest booking",
    description = "Retrieves active alerts for latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Active Alerts Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerAlertsResponse::class),
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
        description = "Prisoner does not exist or has no bookings",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getActiveAlertsForReconciliation(
    @Schema(description = "Offender No AKA prisoner number", example = "A1234AK")
    @PathVariable
    offenderNo: String,
  ): PrisonerAlertsResponse = alertsService.getActiveAlertsForReconciliation(offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/booking-id/{bookingId}/alerts")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets alert for booking",
    description = "Retrieves alerts for a specific booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Alerts Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = BookingAlertsResponse::class),
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
        description = "Booking does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAlertsByBookingId(
    @Schema(description = "Booking id", example = "12345")
    @PathVariable
    bookingId: Long,
  ): BookingAlertsResponse = alertsService.getAlerts(bookingId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/alerts")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an alert on a prisoner",
    description = "Creates an alert on the prisoner's latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Alert Created",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateAlertResponse::class),
          ),
        ],
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
      ApiResponse(
        responseCode = "409",
        description = "Active alert of this type already exists",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createAlert(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: CreateAlertRequest,
  ): CreateAlertResponse = alertsService.createAlert(offenderNo, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/prisoners/booking-id/{bookingId}/alerts/{alertSequence}")
  @Operation(
    summary = "Updates an alert on a prisoner",
    description = "Updates an alert on the specified prisoner's booking which should be the latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Alert Updated",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AlertResponse::class),
          ),
        ],
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
        description = "Alert does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateAlert(
    @Schema(description = "Booking id", example = "1234567")
    @PathVariable
    bookingId: Long,
    @Schema(description = "Alert sequence", example = "3")
    @PathVariable
    alertSequence: Long,
    @RequestBody @Valid
    request: UpdateAlertRequest,
  ): AlertResponse = alertsService.updateAlert(bookingId, alertSequence, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @DeleteMapping("/prisoners/booking-id/{bookingId}/alerts/{alertSequence}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes an alert by bookingId and alert sequence",
    description = "Deletes an prisoner alert. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Alert Deleted",
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
    ],
  )
  fun deleteAlert(
    @Schema(description = "Booking Id", example = "12345")
    @PathVariable
    bookingId: Long,
    @Schema(description = "Alert sequence", example = "3")
    @PathVariable
    alertSequence: Long,
  ): Unit = alertsService.deleteAlert(bookingId, alertSequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/alerts/codes")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an alert code",
    description = "Creates an alert code in the NOMIS reference data. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Alert code Created",
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
        responseCode = "409",
        description = "Code already exits",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createAlertCode(
    @RequestBody @Valid
    request: CreateAlertCode,
  ) = alertsReferenceDataService.createAlertCode(request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/alerts/resynchronise")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Replaces an alerts on a prisoner",
    description = "Replaces all alerts on the prisoner's latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Alerts replaces",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = CreateAlertResponse::class)),
          ),
        ],
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
  fun resynchroniseAlerts(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: List<CreateAlertRequest>,
  ): List<CreateAlertResponse> = alertsService.resynchroniseAlerts(offenderNo, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/alerts/codes/{code}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update an alert code",
    description = "Updates an alert code in the NOMIS reference data, specifically the description. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Alert code updated",
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
        description = "Alert code does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateAlertCode(
    @PathVariable
    code: String,
    @RequestBody @Valid
    request: UpdateAlertCode,
  ) = alertsReferenceDataService.updateAlertCode(code, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/alerts/codes/{code}/reactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update an alert code to be active",
    description = "Updates an alert code in the NOMIS reference data to be active, specifically the description. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Alert code reactivated",
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
        description = "Alert code does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun reactivateAlertCode(
    @PathVariable
    code: String,
  ) = alertsReferenceDataService.reactivateAlertCode(code)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/alerts/codes/{code}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update an alert code to be inactive",
    description = "Updates an alert code in the NOMIS reference data to be inactive, specifically the description. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Alert code deactivated",
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
        description = "Alert code does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deactivateAlertCode(
    @PathVariable
    code: String,
  ) = alertsReferenceDataService.deactivateAlertCode(code)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/alerts/types")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates an alert type",
    description = "Creates an alert type in the NOMIS reference data. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Alert type Created",
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
        responseCode = "409",
        description = "Type already exits",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createAlertType(
    @RequestBody @Valid
    request: CreateAlertType,
  ) = alertsReferenceDataService.createAlertType(request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/alerts/types/{code}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update an alert type",
    description = "Updates an alert type in the NOMIS reference data, specifically the description. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Alert type updated",
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
        description = "Alert type does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateAlertType(
    @PathVariable
    code: String,
    @RequestBody @Valid
    request: UpdateAlertType,
  ) = alertsReferenceDataService.updateAlertType(code, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/alerts/types/{code}/reactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update an alert type to be active",
    description = "Updates an alert type in the NOMIS reference data to be active, specifically the description. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Alert code reactivated",
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
        description = "Alert code does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun reactivateAlertType(
    @PathVariable
    code: String,
  ) = alertsReferenceDataService.reactivateAlertType(code)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/alerts/types/{code}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update an alert type to be inactive",
    description = "Updates an alert type in the NOMIS reference data to be inactive, specifically the description. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Alert type deactivated",
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
        description = "Alert type does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deactivateAlertType(
    @PathVariable
    code: String,
  ) = alertsReferenceDataService.deactivateAlertType(code)
}

@Schema(description = "The list of unique alerts held against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerAlertsResponse(
  val latestBookingAlerts: List<AlertResponse>,
)

@Schema(description = "The list of alerts held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BookingAlertsResponse(
  val alerts: List<AlertResponse>,
)

@Schema(description = "The data held in NOMIS about an alert associated with a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AlertResponse(
  @Schema(description = "The prisoner's bookingId related to this alert")
  val bookingId: Long,
  @Schema(description = "The prisoner's bookingId sequence related to this alert. Used to show if this is on latest bookings")
  val bookingSequence: Long,
  @Schema(description = "The sequence primary key within this booking")
  val alertSequence: Long,
  @Schema(description = "The alert code")
  val alertCode: CodeDescription,
  @Schema(description = "The alert type")
  val type: CodeDescription,
  @Schema(description = "Date alert started")
  val date: LocalDate,
  @Schema(description = "Date alert expired")
  val expiryDate: LocalDate? = null,
  @Schema(description = "true if alert is active and has not expired")
  val isActive: Boolean = true,
  @Schema(description = "true if alert has been verified by another member of staff")
  val isVerified: Boolean = false,
  @Schema(description = "Free format text of person or department that authorised the alert", example = "security")
  val authorisedBy: String? = null,
  @Schema(description = "Free format comment")
  val comment: String? = null,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "A request to create an alert in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateAlertRequest(
  @Schema(description = "The alert code")
  @NotNull
  val alertCode: String,
  @Schema(description = "Date alert started")
  val date: LocalDate,
  @Schema(description = "Date alert expired")
  val expiryDate: LocalDate? = null,
  @Schema(description = "true if alert is active and has not expired")
  val isActive: Boolean = true,
  @Schema(description = "Free format comment")
  val comment: String? = null,
  @Schema(description = "Free format text of person or department that authorised the alert", example = "security")
  val authorisedBy: String? = null,
  @Schema(description = "Username of person that created the record (might also be a system) ")
  val createUsername: String,
)

@Schema(description = "A response after an alert created in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateAlertResponse(
  @Schema(description = "The prisoner's bookingId related to this alert")
  val bookingId: Long,
  @Schema(description = "The sequence primary key within this booking")
  val alertSequence: Long,
  @Schema(description = "The alert code")
  val alertCode: CodeDescription,
  @Schema(description = "The alert type")
  val type: CodeDescription,
)

@Schema(description = "A request to update an alert in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateAlertRequest(
  // TODO: not sure this can be updated in DPS
  @Schema(description = "Date alert started")
  val date: LocalDate,
  @Schema(description = "Date alert expired")
  val expiryDate: LocalDate? = null,
  @Schema(description = "true if alert is active and has not expired")
  val isActive: Boolean = true,
  @Schema(description = "Free format comment")
  val comment: String? = null,
  @Schema(description = "Username of person that update the record (might also be a system) ")
  val updateUsername: String,
  // TODO: not sure this can be updated in DPS
  @Schema(description = "Free format text of person or department that authorised the alert", example = "security")
  val authorisedBy: String? = null,
)

@Schema(description = "A request to create an alert code reference data in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateAlertCode(
  @Schema(description = "The alert code")
  @NotNull
  val code: String,
  @Schema(description = "The parent type code")
  @NotNull
  val typeCode: String,
  @Schema(description = "The alert description")
  @NotNull
  val description: String,
  @Schema(description = "The sequence in a UI list")
  @NotNull
  val listSequence: Int,
)

@Schema(description = "A request to update an alert code reference data in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateAlertCode(
  @Schema(description = "The alert description")
  @NotNull
  val description: String,
)

@Schema(description = "A request to create an alert type reference data in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateAlertType(
  @Schema(description = "The alert type code")
  @NotNull
  val code: String,
  @Schema(description = "The alert type description")
  @NotNull
  val description: String,
  @Schema(description = "The sequence in a UI list")
  @NotNull
  val listSequence: Int,
)

@Schema(description = "A request to update an alert type reference data in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateAlertType(
  @Schema(description = "The alert type description")
  @NotNull
  val description: String,
)
