package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class LocationResource(private val locationService: LocationService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @PostMapping("/locations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new location",
    description = "Creates a new location. Requires role ROLE_NOMIS_LOCATIONS",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(mediaType = "application/json", schema = Schema(implementation = CreateLocationRequest::class)),
      ],
    ),
    responses = [
      ApiResponse(responseCode = "201", description = "Successfully created location"),
      ApiResponse(
        responseCode = "400",
        description = "Invalid data such as prison or parent do not exist etc.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role ROLE_NOMIS_LOCATIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createLocation(
    @RequestBody @Valid
    createLocationRequest: CreateLocationRequest,
  ): LocationIdResponse = locationService.createLocation(createLocationRequest)

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @GetMapping("/locations/{id}")
  @Operation(
    summary = "Get a location",
    description = "Get the location given the id. Requires role ROLE_NOMIS_LOCATIONS",
    responses = [
      ApiResponse(responseCode = "200", description = "Non-association information"),
      ApiResponse(
        responseCode = "404",
        description = "No location exists for this id",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role ROLE_NOMIS_LOCATIONS",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getLocation(
    @Parameter(description = "Location id", example = "12345678", required = true)
    @PathVariable
    id: Long,
  ): LocationResponse = locationService.getLocation(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_LOCATIONS')")
  @GetMapping("/locations/ids")
  @Operation(
    summary = "get locations by filter",
    description = "Retrieves a paged list of composite ids by filter. Requires ROLE_NOMIS_LOCATIONS.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of ids is returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint when role ROLE_NOMIS_LOCATIONS not present",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getLocationsByFilter(
    @PageableDefault(sort = ["locationId"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
  ): Page<LocationIdResponse> = locationService.findIdsByFilter(pageRequest = pageRequest)
}
