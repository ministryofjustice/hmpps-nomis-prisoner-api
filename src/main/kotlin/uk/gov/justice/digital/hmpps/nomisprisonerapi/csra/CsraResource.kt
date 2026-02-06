package uk.gov.justice.digital.hmpps.nomisprisonerapi.csra

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
class CsraResource(private val csraService: CsraService) {
  @PostMapping("/prisoners/{offenderNo}/csra")
  @Operation(
    summary = "Creates a CSRA record for a prisoner",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
  )
  fun createCsra(
    @PathVariable offenderNo: String,
    @RequestBody csraCreateRequest: CsraCreateDto,
  ): CsraCreateResponse = csraService.createCsra(offenderNo, csraCreateRequest)

  @GetMapping("/prisoners/booking-id/{bookingId}/csra/{sequence}")
  @Operation(
    summary = "Get a CSRA record for a prisoner",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
  )
  fun getCsra(
    @Schema(description = "Booking Id", example = "2345678") @PathVariable bookingId: Long,
    @Schema(description = "Sequence within booking", example = "3") @PathVariable sequence: Int,
  ): CsraGetDto = csraService.getCsra(bookingId, sequence)

  @GetMapping("/prisoners/{offenderNo}/csras")
  @Operation(
    summary = "Gets all CSRAs for a prisoner",
    description = "Retrieves all CSRAs for a specific prisoner, for migration. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getCsrasForPrisoner(
    @Schema(description = "Offender No AKA prisoner number", example = "A3745XD")
    @PathVariable
    offenderNo: String,
  ): PrisonerCsrasResponse = csraService.getCsras(offenderNo)
}

@Schema(description = "The list of CSRAs held against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerCsrasResponse(
  val csras: List<CsraGetDto>,
)
