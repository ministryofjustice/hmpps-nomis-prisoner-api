package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

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
@RequestMapping
@PreAuthorize("hasRole('ROLE_NOMIS_PRISON_PERSON')")
class IdentifyingMarkImagesResource(private val service: IdentifyingMarksService) {

  @GetMapping("/identifying-marks/images/{imageId}/details", produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Get an identifying mark image details",
    description = "Retrieves an identifying mark image details. Note this does not include the image itself which is available on a separate endpoint. Requires ROLE_NOMIS_PRISON_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Image details returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = IdentifyingMarkImageDetailsResponse::class),
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
        description = "Image does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIdentifyingMarksImageDetails(
    @Schema(description = "Image id", example = "12345") @PathVariable imageId: Long,
  ): IdentifyingMarkImageDetailsResponse = service.getIdentifyingMarksImageDetails(imageId)

  @GetMapping("/identifying-marks/images/{imageId}/data", produces = [MediaType.IMAGE_JPEG_VALUE])
  @Operation(
    summary = "Get an identifying mark image in JPEG format",
    description = "Retrieves an identifying mark image in JPEG format. Requires ROLE_NOMIS_PRISON_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Image  returned",
        content = [
          Content(
            mediaType = "image/jpeg",
            schema = Schema(implementation = ByteArray::class),
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
        description = "Image does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIdentifyingMarksImageData(
    @Schema(description = "Image id", example = "12345") @PathVariable imageId: Long,
  ): ByteArray = service.getIdentifyingMarksImageData(imageId)
}
