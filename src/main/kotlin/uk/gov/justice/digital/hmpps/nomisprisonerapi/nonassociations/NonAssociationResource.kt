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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException

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
      ApiResponse(responseCode = "201", description = "Successfully created non-association"),
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
  ): CreateNonAssociationResponse = nonAssociationService.createNonAssociation(createNonAssociationRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @PutMapping("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/sequence/{typeSequence}")
  @Operation(
    summary = "Updates an existing non-association",
    description = "Updates an existing non-association. Requires role NOMIS_NON_ASSOCIATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = UpdateNonAssociationRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "200", description = "Successfully amended non-association"),
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
    @Parameter(description = "Offender", example = "A3456GH", required = true)
    @PathVariable
    offenderNo: String,
    @Parameter(description = "Non-association offender", example = "A34578ED", required = true)
    @PathVariable
    nsOffenderNo: String,
    @Parameter(description = "Sequence number. Amend this specific detail record", example = "1", required = true)
    @PathVariable
    typeSequence: Int,
    @RequestBody @Valid
    updateNonAssociationRequest: UpdateNonAssociationRequest,
  ) {
    nonAssociationService.updateNonAssociation(offenderNo, nsOffenderNo, typeSequence, updateNonAssociationRequest)
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @PutMapping("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/sequence/{typeSequence}/close")
  @Operation(
    summary = "Closes an existing non-association",
    description = "Closes an existing non-association. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Success"),
      ApiResponse(
        responseCode = "404",
        description = "Non-association does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Non-association is already closed",
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
    @Parameter(description = "Offender", example = "A3456GH", required = true)
    @PathVariable
    offenderNo: String,
    @Parameter(description = "Non-association offender", example = "A34578ED", required = true)
    @PathVariable
    nsOffenderNo: String,
    @Parameter(description = "Sequence number. Close this specific detail record", example = "2", required = true)
    @PathVariable
    typeSequence: Int,
  ) {
    nonAssociationService.closeNonAssociation(offenderNo, nsOffenderNo, typeSequence)
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @DeleteMapping("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/sequence/{typeSequence}")
  @Operation(
    summary = "Deletes a non-association",
    description = "Deletes the specified non-association detail record. if there was only one, the parent NA record is deleted too. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Success"),
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
  fun deleteNonAssociation(
    @Parameter(description = "Offender", example = "A3456GH", required = true)
    @PathVariable
    offenderNo: String,
    @Parameter(description = "Non-association offender", example = "A34578ED", required = true)
    @PathVariable
    nsOffenderNo: String,
    @Parameter(description = "Sequence number. Close this specific detail record", example = "2", required = true)
    @PathVariable
    typeSequence: Int,
  ) {
    nonAssociationService.deleteNonAssociation(offenderNo, nsOffenderNo, typeSequence)
  }

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @GetMapping("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}")
  @Operation(
    summary = "Get an open non-association",
    description = "Get the open non-association for the two offender numbers. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Non-association information"),
      ApiResponse(
        responseCode = "404",
        description = "No open non-association exists for these offender numbers, or one of the offenders does not exist",
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
    @Parameter(description = "Offender", example = "A3456GH", required = true)
    @PathVariable
    offenderNo: String,
    @Parameter(description = "Non-association offender", example = "A34578ED", required = true)
    @PathVariable
    nsOffenderNo: String,
    @Parameter(description = "Sequence number. If present, get this detail record, otherwise get the open record if there is one.", example = "2")
    @RequestParam("typeSequence", required = false)
    typeSequence: Int?,
  ): NonAssociationResponse =
    try {
      nonAssociationService.getNonAssociation(offenderNo, nsOffenderNo, typeSequence, false).first()
    } catch (e: NoSuchElementException) {
      throw NotFoundException("No open non-association exists for these offender numbers")
    }

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @GetMapping("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/all")
  @Operation(
    summary = "Get all non-associations for the two offender numbers",
    description = "Get all non-associations for the two offender numbers, including expired. Requires role NOMIS_NON_ASSOCIATIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "List of non-associations"),
      ApiResponse(
        responseCode = "404",
        description = "Non-association does not exist, or one of the offenders does not exist",
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
  fun getNonAssociationDetails(
    @Parameter(description = "Offender", example = "A3456GH", required = true)
    @PathVariable
    offenderNo: String,
    @Parameter(description = "Non-association offender", example = "A34578ED", required = true)
    @PathVariable
    nsOffenderNo: String,
  ): List<NonAssociationResponse> =
    nonAssociationService.getNonAssociation(offenderNo, nsOffenderNo, null, true)

  @PreAuthorize("hasRole('ROLE_NOMIS_NON_ASSOCIATIONS')")
  @GetMapping("/non-associations/ids")
  @Operation(
    summary = "get non-associations by filter",
    description = "Retrieves a paged list of composite ids by filter. Requires ROLE_NOMIS_NON_ASSOCIATIONS.",
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
        description = "Forbidden to access this endpoint when role NOMIS_NON_ASSOCIATIONS not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getNonAssociationsByFilter(
    @PageableDefault(sort = ["id.offenderId"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
  ): Page<NonAssociationIdResponse> =
    nonAssociationService.findIdsByFilter(
      pageRequest = pageRequest,
    )
}
