package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class NonAssociationResource(private val nonAssociationService: NonAssociationService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @PostMapping("/non-associations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new non-association",
    description = "Creates a new non-association. Requires role NOMIS_NON_ASSOCIATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = CreateNonAssociationRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Successfully created non-association",
        content = [Content(mediaType = "application/json")],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data such as booking or location do not exist etc.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_NON_ASSOCIATIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createNonAssociation(
    @RequestBody @Valid
    createNonAssociationRequest: CreateNonAssociationRequest,
  ) = nonAssociationService.createNonAssociation(createNonAssociationRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @PutMapping("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}")
  @Operation(
    summary = "Updates an existing non-association",
    description = "Updates an existing non-association. Requires role NOMIS_NON_ASSOCIATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = CreateNonAssociationRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Successfully updated non-association",
        content = [Content(mediaType = "application/json")],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data such as reason or type do not exist etc.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_NON_ASSOCIATIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateNonAssociation(
    @Schema(description = "Offender", example = "A3456GH", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Non-association offender", example = "A34578ED", required = true)
    @PathVariable
    nsOffenderNo: String,
    @RequestBody @Valid
    updateNonAssociationRequest: UpdateNonAssociationRequest,
  ) = nonAssociationService.updateNonAssociation(offenderNo, nsOffenderNo, updateNonAssociationRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @PutMapping("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/close")
  @Operation(
    summary = "Closes an existing non-association",
    description = "Closes an existing non-association. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Success",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateNonAssociationRequest::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_NON_ASSOCIATIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun closeNonAssociation(
    @Schema(description = "Offender", example = "A3456GH", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Non-association offender", example = "A34578ED", required = true)
    @PathVariable
    nsOffenderNo: String,
  ) = nonAssociationService.closeNonAssociation(offenderNo, nsOffenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @GetMapping("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}")
  @Operation(
    summary = "Get an non-association",
    description = "Get an non-association given the two offender numbers. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Non-association information with created id",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateNonAssociationRequest::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_NON_ASSOCIATIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getNonAssociation(
    @Schema(description = "Offender", example = "A3456GH", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Non-association offender", example = "A34578ED", required = true)
    @PathVariable
    nsOffenderNo: String,
  ): NonAssociationResponse =
    nonAssociationService.getNonAssociation(offenderNo, nsOffenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @GetMapping("/non-associations/ids")
  @Operation(
    summary = "get non-associations by filter",
    description = "Retrieves a paged list of incentive composite ids by filter. Requires ROLE_NOMIS_NON_ASSOCIATIONS.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of composite ids are returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_INCENTIVES not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getNonAssociationsByFilter(
    @PageableDefault(sort = ["eventId"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "prisonIds", required = false)
    @Parameter(
      description = "Filter results by prison ids (returns all prisons if not specified)",
      example = "['MDI','LEI']",
    )
    prisonIds: List<String>?,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by non-associations that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by non-associations that were created on or before the given date",
      example = "2022-04-11",
    )
    toDate: LocalDate?,
  ): Page<NonAssociationIdResponse> =
    nonAssociationService.findIdsByFilter(
      pageRequest = pageRequest,
      NonAssociationFilter(toDate = toDate, fromDate = fromDate),
    )
}
