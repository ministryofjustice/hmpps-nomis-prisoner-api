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
@RequestMapping("/prisons", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class PrisonResource(val prisonService: PrisonService) {
  @GetMapping
  @Operation(
    summary = "Retrieve a list of active prisons.",
    description = """
      Retrieve a list of active prisons. It only returns prisons that are active and does not return 
      prisons with any of the "special" codes: "*ALL*", "OUT", "TRN" or "ZZGHI".
      Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW
       """,
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
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
  fun getActivePrisons(): List<Prison> = prisonService.getAllActivePrisons()

  @GetMapping("/{prisonId}/incentive-levels")
  @Operation(
    summary = "Retrieve a list of active incentive levels for a prison",
    description = "Retrieve a list of active incentive levels for a prison. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
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
  fun getPrisonIncentiveLevels(
    @Schema(description = "The prison ID") @PathVariable prisonId: String,
  ): List<IncentiveLevel> = prisonService.getPrisonIepLevels(prisonId)
}

@Schema(description = "An incentive levels")
data class IncentiveLevel(
  @Schema(description = "The incentive level code", example = "STD")
  val code: String,
  @Schema(description = "The incentive level description", example = "Standard")
  val description: String,
)

@Schema(description = "Prison")
data class Prison(
  @Schema(description = "The prison Id", example = "MDI")
  val id: String,
  @Schema(description = "The description for the prison", example = "Moorland")
  val description: String,
)
