package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visits.VisitResponse

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class AdjudicationResource(
  private val adjudicationService: AdjudicationService,
) {

  @PreAuthorize("hasRole('ROLE_NOMIS_ADJUDICATIONS')")
  @GetMapping("/adjudications/adjudication-number/{adjudicationNumber}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get adjudication by adjudication number",
    description = "Retrieves an adjudication by the adjudication number.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Adjudication Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitResponse::class))],
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
        responseCode = "404",
        description = "Adjudication does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAdjudication(
    @Schema(description = "Nomis Visit Id", example = "12345", required = true)
    @PathVariable
    adjudicationNumber: Long,
  ): AdjudicationResponse =
    adjudicationService.getAdjudication(adjudicationNumber)
}
