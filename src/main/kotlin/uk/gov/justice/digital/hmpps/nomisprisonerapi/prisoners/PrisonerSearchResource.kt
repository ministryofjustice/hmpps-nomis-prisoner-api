package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(value = ["/search"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__PRISONER_SEARCH_R')")
class PrisonerSearchResource(private val prisonerSearchService: PrisonerSearchService) {
  @GetMapping("/prisoners/id-ranges")
  @Operation(
    summary = "Gets every size prisoner root offender ids for prisoner search.",
    description = """Returns a list of root offender id ranges for prisoners. Each list item has a from and to 
      root offender id, with the number of prisoners between the from and to equal to the specified size. 
      Requires role NOMIS_PRISONER_API__PRISONER_SEARCH_R.""",
    responses = [
      ApiResponse(responseCode = "200", description = "list of root offender id ranges"),
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__PRISONER_SEARCH_R not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAllPrisonersIdRanges(
    @RequestParam(value = "active", required = true)
    @Parameter(
      description = "When true only return active prisoners currently in prison else all prisoners are returned.",
    )
    active: Boolean,
    @RequestParam(value = "size", defaultValue = "1000")
    @Parameter(description = "Number of prisoners to get")
    pageSize: Int,
  ): List<RootOffenderIdRange> = prisonerSearchService.findRootOffenderIdRanges(active, pageSize)

  @GetMapping("/prisoners/ids")
  @Operation(
    summary = "Gets every prison number between range for prisoner search.",
    description = """Returns a list of prison numbers for root offender ids greater than the specified
      fromRootOffenderId and less than or equal to the specified toRootOffenderId.
      Requires role NOMIS_PRISONER_API__PRISONER_SEARCH_R.""",
    responses = [
      ApiResponse(responseCode = "200", description = "list of prison numbers"),
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__PRISONER_SEARCH_R not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAllPrisonersInRange(
    @RequestParam(value = "active", required = true)
    @Parameter(
      description = "When true only return active prisoners currently in prison else all prisoners are returned.",
    )
    active: Boolean,
    @RequestParam(value = "fromRootOffenderId", required = true)
    @Parameter(description = "Return prisoners with root offender id greater than this value.")
    fromRootOffenderId: Long,
    @RequestParam(value = "toRootOffenderId", required = true)
    @Parameter(description = "Return prisoners with root offender id less than or equal to this value.")
    toRootOffenderId: Long,
  ): List<String> = prisonerSearchService.findPrisonNumbersInRange(active, fromRootOffenderId, toRootOffenderId)
}

@Schema(description = "Root offender ID range.")
data class RootOffenderIdRange(
  @Schema(description = "The lowest NOMIS rootOffenderId in the range", example = "1234567")
  val fromRootOffenderId: Long,
  @Schema(description = "The highest NOMIS rootOffenderId in the range", example = "1234567")
  val toRootOffenderId: Long,
)
