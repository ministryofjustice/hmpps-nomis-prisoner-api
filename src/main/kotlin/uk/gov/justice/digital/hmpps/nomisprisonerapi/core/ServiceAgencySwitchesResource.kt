package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

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
class ServiceAgencySwitchesResource(private val service: ServiceAgencySwitchesService) {

  @PreAuthorize("hasRole('ROLE_SYNCHRONISATION_REPORTING')")
  @GetMapping("/service-prisons/{serviceCode}")
  @Operation(
    summary = "Retrieve a list of prisons switched on for the service",
    description = "Retrieves all prisons switched on for the service code, or an empty list if there are none. Requires role SYNCHRONISATION_REPORTING",
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
        description = "Forbidden, requires role NOMIS_ACTIVITIES",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getServicePrisons(
    @Schema(name = "The code of the service from the EXTERNAL_SERVICES table") @PathVariable serviceCode: String,
  ): List<PrisonDetails> =
    service.getServicePrisons(serviceCode)
}

@Schema(description = "A prison")
data class PrisonDetails(
  @Schema(description = "The prison code", example = "BXI")
  val prisonId: String,
  @Schema(description = "The prison name", example = "Brixton")
  val name: String,
)
