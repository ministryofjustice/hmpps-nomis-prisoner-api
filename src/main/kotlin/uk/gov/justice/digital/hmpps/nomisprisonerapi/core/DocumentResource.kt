package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse

@RestController
@Validated
@RequestMapping(value = ["/documents"])
class DocumentResource(private val documentService: DocumentService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_DOCUMENTS')")
  @GetMapping("/{id}", produces = [APPLICATION_OCTET_STREAM_VALUE])
  @Operation(
    summary = "Retrieve a document",
    description = "Retrieve a document by its id. Requires role NOMIS_DOCUMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_DOCUMENTS",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Document not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getDocument(
    @Schema(description = "The document id") @PathVariable id: Long,
  ) = documentService.getDocumentById(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_DOCUMENTS')")
  @GetMapping("/booking/{bookingId}/template/{templateName}", produces = [APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Retrieve a list of document ids",
    description = "Retrieve a list of document ids searching by booking id and template name. Requires role NOMIS_DOCUMENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_DOCUMENTS",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getDocumentIds(
    @Schema(description = "The booking id") @PathVariable bookingId: Long,
    @Schema(description = "The template name") @PathVariable templateName: String,
  ) = documentService.findAllIds(bookingId, templateName)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Document id")
data class DocumentIdResponse(
  @Schema(description = "The document id", required = true)
  val documentId: Long,
)
