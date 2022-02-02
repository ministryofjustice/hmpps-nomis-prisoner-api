package uk.gov.justice.digital.hmpps.nomisprisonerapi.resource

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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CancelVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.VisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.service.VisitService
import javax.validation.Valid
import javax.validation.constraints.Pattern

const val OFFENDER_NO_PATTERN = "[A-Z]\\d{4}[A-Z]{2}"

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class VisitResource(private val visitService: VisitService) {

  @PreAuthorize("hasRole('ROLE_UPDATE_NOMIS')")
  @PostMapping("/prisoners/{offenderNo}/visits")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new visit",
    description = "Creates a new visit and decrements the visit balance.",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateVisitRequest::class)
        )
      ]
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Visit information with created id"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Prison or person ids do not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "offenderNo does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
    ]
  )
  fun createVisit(
    @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
    @PathVariable
    @Pattern(regexp = OFFENDER_NO_PATTERN)
    offenderNo: String,
    @RequestBody @Valid createVisitRequest: CreateVisitRequest
  ): CreateVisitResponse =
    visitService.createVisit(offenderNo, createVisitRequest)

  @PreAuthorize("hasRole('ROLE_UPDATE_NOMIS')")
  @PutMapping("/prisoners/{offenderNo}/visits/vsipVisitId/{vsipVisitId}/cancel")
  @Operation(
    summary = "Cancel a visit",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit cancelled"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid cancellation reason",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "VSIP visit id not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
    ]
  )
  fun cancelVisit(
    @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
    @PathVariable
    @Pattern(regexp = OFFENDER_NO_PATTERN)
    offenderNo: String,
    @Schema(description = "VSIP Visit Id", required = true)
    @PathVariable
    vsipVisitId: String,
    @RequestBody @Valid cancelVisitRequest: CancelVisitRequest
  ) {
    visitService.cancelVisit(offenderNo, vsipVisitId, cancelVisitRequest)
  }

  @PreAuthorize("hasRole('ROLE_READ_NOMIS')")
  @GetMapping("/visits/{visitId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get visit",
    description = "Retrieves a visit by id.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Visit Information Returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = VisitResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "visit does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
    ]
  )
  fun getVisit(
    @Schema(description = "Visit Id", example = "12345", required = true)
    @PathVariable
    visitId: Long,
  ): VisitResponse =
    visitService.getVisit(visitId)
}
