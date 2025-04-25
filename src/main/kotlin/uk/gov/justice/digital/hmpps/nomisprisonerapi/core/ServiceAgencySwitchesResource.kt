package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException

@RestController
@Validated
@RequestMapping("/service-prisons/", produces = [MediaType.APPLICATION_JSON_VALUE])
class ServiceAgencySwitchesResource(private val service: ServiceAgencySwitchesService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/{serviceCode}")
  @Operation(
    summary = "Retrieve a list of prisons switched on for the service code",
    description = """Returns a list of prisons switched on for the service code.
      A special prisonId of `*ALL*` is used to designate that the service is switched on for all prisons.
      Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
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
    ],
  )
  fun getServicePrisons(
    @Schema(description = "The code of the service from the EXTERNAL_SERVICES table") @PathVariable serviceCode: String,
  ): List<PrisonDetails> = service.getServicePrisons(serviceCode)

  @PreAuthorize("hasAnyRole('ROLE_NOMIS_ACTIVITIES', 'ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/{serviceCode}/prison/{prisonId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Returns if the service is switched on for the specified service code / prison id.",
    description = """Returns 204 if the service is switched on for the service code / prison id combination.
    If the service is not switched on then 404 is returned.
    This endpoint also takes into account the special `*ALL*` prison id - if the service code has a prison entry of
    `*ALL*` then the service is deemed to be switched on for all prisons and will therefore return 204 irrespective of the
    prison id that is passed in.
    Requires role NOMIS_ACTIVITIES or ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(responseCode = "204", description = "Service is switched on for the service code and prison id."),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
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
        description = "The service code does not exist or the service is not switched on for the prison.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun checkServicePrison(
    @Schema(description = "The code of the service from the EXTERNAL_SERVICES table", example = "ACTIVITY") @PathVariable serviceCode: String,
    @Schema(description = "The id of the prison", example = "MDI") @PathVariable prisonId: String,
  ) {
    if (!service.checkServicePrison(serviceCode, prisonId)) {
      throw NotFoundException("Service $serviceCode not turned on for prison $prisonId")
    }
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/{serviceCode}/prisoner/{prisonNumber}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Check if a service is turned on for the prison relating to this prisoner",
    description = "Check if the prisoner's current prison is turned on for a service. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
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
        description = "Not Found, the service is not turned on for the prison",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun checkServicePrisonForPrisoner(
    @Schema(description = "The code of the service from the EXTERNAL_SERVICES table", example = "ACTIVITY") @PathVariable serviceCode: String,
    @Schema(description = "Offender No AKA prisoner number", example = "A1234BC") @PathVariable prisonNumber: String,
  ) {
    if (!service.checkServicePrisonForPrisoner(serviceCode, prisonNumber)) {
      throw NotFoundException("Service $serviceCode not turned on for prisoner $prisonNumber")
    }
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_ACTIVITIES')")
  @PostMapping("{serviceCode}/prison/{prisonId}")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Turn on a service for a prison",
    description = "Turn on a service for a prison. Requires role NOMIS_ACTIVITIES",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
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
        description = "Forbidden, requires role NOMIS_ACTIVITIES",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not Found, the service or prison do not exist",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun createServicePrison(
    @Schema(description = "The code of the service from the EXTERNAL_SERVICES table", example = "ACTIVITY") @PathVariable serviceCode: String,
    @Schema(description = "The id of the prison", example = "MDI") @PathVariable prisonId: String,
  ) = service.createServicePrison(serviceCode, prisonId)
}

@Schema(description = "A prison")
data class PrisonDetails(
  @Schema(description = "The prison code", example = "BXI")
  val prisonId: String,
  @Schema(description = "The prison name", example = "Brixton")
  val name: String,
)
