package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class AppointmentsResource(private val appointmentService: AppointmentService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @PostMapping("/appointments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new appointment",
    description = "Creates a new appointment. Requires role NOMIS_APPOINTMENTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Appointment information with created id",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data such as booking or location do not exist etc.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_APPOINTMENTS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createAppointment(
    @RequestBody @Valid
    createAppointmentRequest: CreateAppointmentRequest,
  ): CreateAppointmentResponse =
    appointmentService.createAppointment(createAppointmentRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @PutMapping("/appointments/{nomisEventId}")
  @Operation(
    summary = "Updates an existing appointment",
    description = "Updates an existing appointment. Requires role NOMIS_APPOINTMENTS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Success",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Event id does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data such as location or subtype do not exist etc.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_APPOINTMENTS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateAppointment(
    @Schema(description = "NOMIS event Id", example = "1234567", required = true)
    @PathVariable
    nomisEventId: Long,
    @RequestBody @Valid
    updateAppointmentRequest: UpdateAppointmentRequest,
  ) = appointmentService.updateAppointment(nomisEventId, updateAppointmentRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @PutMapping("/appointments/{nomisEventId}/cancel")
  @Operation(
    summary = "Cancels an existing appointment",
    description = "Cancels an existing appointment. Requires role NOMIS_APPOINTMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Success",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Event id does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_APPOINTMENTS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun cancelAppointment(
    @Schema(description = "NOMIS event Id", example = "1234567", required = true)
    @PathVariable
    nomisEventId: Long,
  ) = appointmentService.cancelAppointment(nomisEventId)

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/appointments/{nomisEventId}")
  @Operation(
    summary = "Deletes an existing appointment",
    description = "Deletes an existing appointment by actually deleting from the table. Intended for appointments created in error. Requires role NOMIS_APPOINTMENTS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Success",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Event id does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_APPOINTMENTS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun deleteAppointment(
    @Schema(description = "NOMIS event Id", example = "1234567", required = true)
    @PathVariable
    nomisEventId: Long,
  ) = appointmentService.deleteAppointment(nomisEventId)

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @GetMapping("/appointments/booking/{bookingId}/location/{locationId}/start/{dateTime}")
  @Operation(
    summary = "Get an appointment",
    description = "Get an appointment given the booking id, internal location, date and start time. Requires role NOMIS_APPOINTMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment information with created id",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking, location and timestamp combination does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_APPOINTMENTS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAppointment(
    @Schema(description = "NOMIS booking Id", example = "1234567", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Appointment room internal location Id", example = "1234567", required = true)
    @PathVariable
    locationId: Long,
    @Schema(description = "Appointment date and start time", example = "2023-02-27T14:40", required = true)
    @PathVariable
    dateTime: LocalDateTime,
  ): AppointmentResponse =
    appointmentService.getAppointment(bookingId, locationId, dateTime)

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @GetMapping("/appointments/{eventId}")
  @Operation(
    summary = "Get appointment by event id",
    description = "Get an appointment given the unique event id. Requires role NOMIS_APPOINTMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Appointment information with created id",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking, location and timestamp combination does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_APPOINTMENTS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAppointmentById(
    @Schema(description = "Event Id", example = "12345678", required = true)
    @PathVariable
    eventId: Long,
  ): AppointmentResponse =
    appointmentService.getAppointment(eventId)

  @PreAuthorize("hasRole('ROLE_NOMIS_APPOINTMENTS')")
  @GetMapping("/appointments/ids")
  @Operation(
    summary = "get appointments by filter",
    description = "Retrieves a paged list of incentive composite ids by filter. Requires ROLE_NOMIS_APPOINTMENTS.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of composite ids are returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAppointmentsByFilter(
    @PageableDefault(sort = ["eventId"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "prisonIds", required = false)
    @Parameter(
      description = "Filter results by prison ids (returns all prisons if not specified)",
      example = "['MDI','LEI']",
    )
    prisonIds: List<String>?,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by appointments that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by appointments that were created on or before the given date",
      example = "2022-04-11",
    )
    toDate: LocalDate?,
  ): Page<AppointmentIdResponse> =
    appointmentService.findIdsByFilter(
      pageRequest = pageRequest,
      AppointmentFilter(prisonIds = prisonIds ?: listOf(), toDate = toDate, fromDate = fromDate),
    )
}
