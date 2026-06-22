package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

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
class PropertyResource(private val propertyService: PropertyService) {
  @PostMapping("/property-containers")
  @Operation(
    summary = "Creates a prisoner property container record",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
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
    @RequestBody createRequest: PropertyContainerCreateDto,
  ): CreatePropertyResponse = propertyService.createProperty(createRequest)

  @GetMapping("/property-containers/{id}")
  @Operation(
    summary = "Get a prisoner property container record",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PropertyContainerGetDto::class)),
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
  ): PropertyContainerGetDto = propertyService.getProperty(id)
}
