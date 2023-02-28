package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
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
        Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class))
      ]
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Appointment information with created id",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class))
        ]
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data such as booking or location do not exist etc.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_APPOINTMENTS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  fun createAppointment(
    @RequestBody @Valid createAppointmentRequest: CreateAppointmentRequest
  ): CreateAppointmentResponse =
    appointmentService.createAppointment(createAppointmentRequest)

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
          Content(mediaType = "application/json", schema = Schema(implementation = CreateAppointmentRequest::class))
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking, location and timestamp combination does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_APPOINTMENTS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  fun getAppointment(
    @Schema(description = "NOMIS booking Id", example = "1234567", required = true)
    @PathVariable bookingId: Long,
    @Schema(description = "Appointment room internal location Id", example = "1234567", required = true)
    @PathVariable locationId: Long,
    @Schema(description = "Appointment date and start time", example = "2023-02-27T14:40", required = true)
    @PathVariable dateTime: LocalDateTime,
  ): AppointmentResponse =
    appointmentService.getAppointment(bookingId, locationId, dateTime)
}
