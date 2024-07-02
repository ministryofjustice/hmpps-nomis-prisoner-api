package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.factors

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDateTime

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
@RequestMapping(value = ["/csip/factors"], produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPFactorResource(private val csipFactorService: CSIPFactorService) {

  @GetMapping("/{csipFactorId}")
  @Operation(
    summary = "Get CSIP factor details",
    description = "Gets csip factor details. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
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
        description = "Forbidden, requires role NOMIS_CSIP",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getCSIPFactor(
    @Schema(description = "CSIP Factor id") @PathVariable csipFactorId: Long,
  ) = csipFactorService.getCSIPFactor(csipFactorId)

  @DeleteMapping("/{csipFactorId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a csip factor",
    description = "Deletes a csip factor. Requires ROLE_NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Csip factor Deleted",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CSIP",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteCSIPFactor(
    @Schema(description = "CSIP Factor Id", example = "12345")
    @PathVariable
    csipFactorId: Long,
  ): Unit = csipFactorService.deleteCSIPFactor(csipFactorId)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CSIPFactorResponse(
  @Schema(description = "Factor type id")
  val id: Long,
  @Schema(description = "Contributory Factor")
  val type: CodeDescription,
  @Schema(description = "Factor comment")
  val comment: String?,
  @Schema(description = "The date and time the report was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the report")
  val createdBy: String,
  @Schema(description = "The date and time the report was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the report")
  val lastModifiedBy: String?,
)
