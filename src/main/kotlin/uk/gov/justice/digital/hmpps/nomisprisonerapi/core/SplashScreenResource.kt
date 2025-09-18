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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SplashScreenResource(private val service: SplashScreenService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/splash-screens/{moduleName}")
  @Operation(
    summary = "Retrieve a list of prisons and their associated screen conditions (if any set) for the screen",
    description = "Retrieves all prisons switched on for the screen (module) name, or an empty list if there are none. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
  fun getScreenConditions(
    @Schema(description = "The name of the screen (module)", example = "OIDINCRS")
    @PathVariable moduleName: String,
  ): SplashScreenDto = service.getSplashScreen(moduleName)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/splash-screens/{moduleName}/blocked")
  @Operation(
    summary = "Retrieve a list of blocked prison ids for the screen",
    description = "Retrieves a list of blocked prison ids for the screen (module) name or **ALL** if all prisons, or an empty list if there are none blocked. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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
      ApiResponse(
        responseCode = "404",
        description = "Not found if the screen (module) name does not exist",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getBlockedPrisons(
    @Schema(description = "The name of the screen (module)", example = "OIDINCRS")
    @PathVariable moduleName: String,
  ): List<PrisonDto> = service.getBlockedPrisons(moduleName)
}

data class PrisonDto(
  @Schema(description = "The prisonId or **ALL**", example = "MDI but can be **ALL** for all")
  val prisonId: String,
)

@Schema(description = "Splash Screen Access Condition details")
data class SplashConditionDto(
  @Schema(description = "The prisonId or **ALL**", example = "MDI but can be **ALL** for all")
  val prisonId: String,
  @Schema(description = "Whether access to the screen is blocked", example = "true")
  val accessBlocked: Boolean,
  @Schema(description = "The type of condition set on the screen", example = "CASELOAD")
  val type: CodeDescription,
)

@Schema(description = "Splash screen details")
data class SplashScreenDto(
  @Schema(description = "The name of the module/screen", example = "OIDINCRS")
  val moduleName: String,

  @Schema(description = "The type of access - YES, NO, COND")
  val accessBlockedType: CodeDescription,

  @Schema(description = "The text shown when a screen is accessible but will shortly be turned off", example = "This screen will be turned off next month.")
  val warningText: String?,

  @Schema(description = "The text shown when a screen is blocked", example = "This screen is no longer accessible, use DPS.")
  val blockedText: String?,

  @Schema(description = "Prison access conditions")
  val conditions: List<SplashConditionDto>,
)
