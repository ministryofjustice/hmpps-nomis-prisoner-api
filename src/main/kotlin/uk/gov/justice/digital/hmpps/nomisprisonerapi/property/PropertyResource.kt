package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
class PropertyResource(private val propertyService: PropertyService) {
  @PostMapping("/property-containers")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a prisoner property container record",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreatePropertyResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid agency or user",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun create(
    @RequestBody createRequest: PropertyContainerCreateRequest,
  ): CreatePropertyResponse = propertyService.createProperty(createRequest)

  @GetMapping("/property-containers/{id}")
  @Operation(
    summary = "Get a prisoner property container record",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PropertyContainerGetResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "prisoner property container record does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun get(
    @Schema(description = "property container Id", example = "2345678") @PathVariable id: Long,
  ): PropertyContainerGetResponse = propertyService.getProperty(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/property-containers/ids")
  @Operation(
    summary = "get property container by filter",
    description = "Retrieves a paged list of property container ids by filter. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of ids are returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getPropertyContainersByFilter(
    @PageableDefault(sort = ["propertyContainerId"], direction = Sort.Direction.ASC)
    @ParameterObject
    pageRequest: Pageable,
    @RequestParam(value = "prisonIds", required = false)
    @Parameter(description = "Filter results by prison ids", example = "['MDI','LEI']")
    prisonIds: List<String>?,
  ): Page<ContainerIdResponse> = propertyService.findIdsByFilter(
    pageRequest = pageRequest,
    PropertyFilter(prisonIds = prisonIds),
  )
}
