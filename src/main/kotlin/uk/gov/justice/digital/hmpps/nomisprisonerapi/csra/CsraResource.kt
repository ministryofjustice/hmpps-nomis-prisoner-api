package uk.gov.justice.digital.hmpps.nomisprisonerapi.csra

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CsraResource(private val csraService: CsraService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/csra/{offenderNo}")
  @Operation(
    summary = "Creates a CSRA record for a prisoner",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
  )
  fun createCsra(
    @PathVariable offenderNo: String,
    @RequestBody csraCreateRequest: CsraDto,
  ): CsraCreateResponse = csraService.createCsra(offenderNo, csraCreateRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/csra/booking/{bookingId}/sequence/{sequence}")
  @Operation(
    summary = "Get a CSRA record for a prisoner",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
  )
  fun getCsra(
    @Schema(description = "Booking Id", example = "2345678") @PathVariable bookingId: Long,
    @Schema(description = "Sequence within booking", example = "3") @PathVariable sequence: Int,
  ): CsraDto = csraService.getCsra(bookingId, sequence)
}
