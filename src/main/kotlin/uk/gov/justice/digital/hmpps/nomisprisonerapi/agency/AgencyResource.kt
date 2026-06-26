package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

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
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class AgencyResource(private val agencyService: AgencyService) {

  @GetMapping("/prison/{prisonId}")
  @Operation(
    summary = "Gets details of a prison",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Prison details",
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
        description = "Prison not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrison(
    @PathVariable
    @Schema(description = "Prison id (aka agencyId)", example = "WWI")
    prisonId: String,
  ) = agencyService.getPrison(prisonId)

  @GetMapping("/agency/{agencyId}")
  @Operation(
    summary = "Gets details of a agency",
    description = "Requires ROLE_NOMIS_AGENCYER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency details",
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
        description = "Agency not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAgency(
    @PathVariable
    @Schema(description = "Agency id (aka agencyId)", example = "WWI")
    agencyId: String,
  ) = agencyService.getAgency(agencyId)

  @GetMapping("/agency-location/{agencyId}")
  @Operation(
    summary = "Gets details of a agency",
    description = "Requires ROLE_NOMIS_AGENCYER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency details",
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
        description = "Agency not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAgencyLocation(
    @PathVariable
    @Schema(description = "Agency id (aka agencyId)", example = "WWI")
    agencyId: String,
  ) = agencyService.getAgencyLocation(agencyId)
}

data class PrisonResponse(
  @Schema(description = "The prison id", example = "WWI")
  val prisonId: String,
  @Schema(description = "Name of prison", example = "WANDSWORTH (HMP)")
  val description: String,
  @Schema(description = "Area district")
  val district: CodeDescription?,
  @Schema(description = "Indicates if still used", example = "true")
  val active: Boolean,
  @Schema(description = "Date no longer active", example = "2020-01-01")
  val deactivationDate: LocalDate?,
  @Schema(description = "Indicates if data is allowed to be updated", example = "true")
  val updateAllowed: Boolean,
  @Schema(description = "Name of contact at agency", example = "John Smith")
  val contactName: String?,
)

data class AgencyResponse(
  @Schema(description = "The agency id", example = "LCSY02")
  val agencyId: String,
  @Schema(description = "Name of agency", example = "Blackburn YOT")
  val description: String,
  @Schema(description = "Geographic district")
  val district: CodeDescription?,
  @Schema(description = "Agency type")
  val type: CodeDescription,
  @Schema(description = "Indicates if still used", example = "true")
  val active: Boolean,
  @Schema(description = "Date no longer active", example = "2020-01-01")
  val deactivationDate: LocalDate?,
  @Schema(description = "Indicates if data is allowed to be updated", example = "true")
  val updateAllowed: Boolean,
  @Schema(description = "Name of contact at agency", example = "John Smith")
  val contactName: String?,
)

data class AgencyLocationResponse(
  @Schema(description = "The agency id", example = "LCSY02")
  val agencyId: String,
  @Schema(description = "Name of agency", example = "Blackburn YOT")
  val description: String,
  @Schema(description = "Agency type")
  val type: CodeDescription,
  @Schema(description = "Indicates if still used", example = "true")
  val active: Boolean,
  @Schema(description = "Date no longer active", example = "2020-01-01")
  val deactivationDate: LocalDate?,
  @Schema(description = "Indicates if data is allowed to be updated", example = "true")
  val updateAllowed: Boolean,
  @Schema(description = "Name of contact at agency", example = "John Smith")
  val contactName: String?,
)
