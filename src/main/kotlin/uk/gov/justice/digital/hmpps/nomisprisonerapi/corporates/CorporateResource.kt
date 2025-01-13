package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import com.fasterxml.jackson.annotation.JsonInclude
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CorporateResource(private val corporateService: CorporateService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @GetMapping("/corporates/{corporateId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a corporate by corporateId Id",
    description = "Retrieves a corporate and details. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Corporate Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CorporateOrganisation::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Corporate does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCorporateById(
    @Schema(description = "Corporate Id", example = "12345")
    @PathVariable
    corporateId: Long,
  ): CorporateOrganisation = corporateService.getCorporateById(corporateId)
}

@Schema(description = "The data held in NOMIS about a corporate entity")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorporateOrganisation(
  @Schema(description = "Unique NOMIS Id of corporate")
  val id: Long,
  @Schema(description = "The corporate name", example = "Boots")
  val name: String,
)
