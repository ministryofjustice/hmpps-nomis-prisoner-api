package uk.gov.justice.digital.hmpps.nomisprisonerapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
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
class SentenceAdjustmentResource {
  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/sentence-adjustments/{sentenceAdjustmentId}")
  @Operation(
    summary = "get specific sentence adjustment",
    description = "Requires role NOMIS_SENTENCING. Retrieves a sentence adjustment by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the sentence adjustment details"
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
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
    ]
  )
  fun getSentenceAdjustment(
    @Schema(description = "Sentence adjustment id", example = "12345", required = true)
    @PathVariable
    sentenceAdjustmentId: Long,
  ): SentenceAdjustmentResponse = SentenceAdjustmentResponse(sentenceAdjustmentId, 1, 1)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PostMapping("/prisoners/booking-id/{bookingId}/sentences/{sentenceSequence}/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new sentence adjustment",
    description = "Required role NOMIS_SENTENCING Creates a new sentence adjustment (aka Debit/Credit). Key dates will not be recalculated as a side effect of this operation",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateSentenceAdjustmentRequest::class)
        )
      ]
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created Sentence adjustment id"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Supplied data is invalid",
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
        responseCode = "403",
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking or sentence sequence do not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
    ]
  )
  fun createSentenceAdjustment(
    @Schema(description = "Booking Id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Sentence sequence number", example = "1", required = true)
    @PathVariable
    sentenceSequence: Long,
    @RequestBody @Valid request: CreateSentenceAdjustmentRequest
  ): CreateSentenceAdjustmentResponse =
    CreateSentenceAdjustmentResponse(1L)
}

@Schema(description = "Create sentence adjustment response")
data class CreateSentenceAdjustmentResponse(
  val sentenceAdjustmentId: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sentence adjustment [TODO add more fields]")
data class SentenceAdjustmentResponse(
  @Schema(description = "The sentence adjustment id", required = true)
  val sentenceAdjustmentId: Long,
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "The sequence of the sentence within this booking", required = true)
  val sentenceSequence: Long,
)

@Schema(description = "Sentence adjustment [TODO add more fields]")
data class CreateSentenceAdjustmentRequest(
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "The sequence of the sentence within this booking", required = true)
  val sentenceSequence: Long,
)
