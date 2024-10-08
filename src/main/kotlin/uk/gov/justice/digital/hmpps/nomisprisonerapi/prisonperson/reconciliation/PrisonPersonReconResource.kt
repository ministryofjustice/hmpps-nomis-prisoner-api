package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.reconciliation

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
class PrisonPersonReconResource(
  private val service: PrisonPersonReconService,
) {

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISON_PERSON')")
  @GetMapping("/prisoners/{offenderNo}/prison-person/reconciliation")
  @Operation(
    summary = "Get prison person reconciliation details for a prisoner",
    description = "Retrieves reconciliation details used to check NOMIS and DPS are aligned. Requires ROLE_NOMIS_PRISON_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Physical Attributes Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonPersonReconciliationResponse::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISON_PERSON",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getReconciliation(
    @Schema(description = "Offender number", example = "A1234AA") @PathVariable offenderNo: String,
  ): PrisonPersonReconciliationResponse = service.getReconciliation(offenderNo)
}
