package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonerHoldProjection

@RestController
@Validated
@RequestMapping(value = ["/finance/prisoners/holds"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class PrisonerHoldsesource(
  private val prisonerHoldsService: PrisonerHoldsService,
) {
  @GetMapping("/{prisonNumber}")
  @Operation(
    summary = "Gets all holds for a prisoner",
    description = "Gets all outstanding holds (HOA and WHF) for a prisoner by prisonNumber. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(responseCode = "200", description = "paged list of prisoner ids"),
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrisonerHolds(
    @Schema(description = "Prison number aka nomisId", example = "A1234BC")
    @PathVariable
    prisonNumber: String,
  ): List<PrisonerHoldProjection> = prisonerHoldsService.getPrisonerHolds(prisonNumber)

  @GetMapping("/rootOffenderId/{rootOffenderId}")
  @Operation(
    summary = "Gets all holds for a prisoner using their root offenderId",
    description = "Gets all outstanding holds (HOA and WHF) for a prisoner by rootOffenderId. Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(responseCode = "200", description = "paged list of prisoner ids"),
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getPrisonerHolds(
    @Schema(description = "rootOffenderId", example = "123456")
    @PathVariable
    rootOffenderId: Long,
  ): List<PrisonerHoldProjection> = prisonerHoldsService.getPrisonerHolds(rootOffenderId)
}
