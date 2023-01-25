package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
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
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SentencingAdjustmentResource(private val sentencingAdjustmentService: SentencingAdjustmentService) {
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
  ): SentenceAdjustmentResponse = sentencingAdjustmentService.getSentenceAdjustment(sentenceAdjustmentId)

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
  ): CreateAdjustmentResponse =
    sentencingAdjustmentService.createSentenceAdjustment(bookingId, sentenceSequence, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/key-date-adjustments/{keyDateAdjustmentId}")
  @Operation(
    summary = "get specific key date adjustment",
    description = "Requires role NOMIS_SENTENCING. Retrieves a key date adjustment by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the key date adjustment details"
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
  fun getKeyDateAdjustment(
    @Schema(description = "Key date adjustment id", example = "12345", required = true)
    @PathVariable
    keyDateAdjustmentId: Long,
  ): KeyDateAdjustmentResponse = sentencingAdjustmentService.getKeyDateAdjustment(keyDateAdjustmentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PostMapping("/prisoners/booking-id/{bookingId}/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new key date adjustment",
    description = "Required role NOMIS_SENTENCING Creates a new key date adjustment. Key dates will be recalculated as a side effect of this operation",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateKeyDateAdjustmentRequest::class)
        )
      ]
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created key date adjustment"
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
        description = "Booking does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
    ]
  )
  fun createKeyDateAdjustment(
    @Schema(description = "Booking Id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @RequestBody @Valid request: CreateKeyDateAdjustmentRequest
  ): CreateAdjustmentResponse =
    sentencingAdjustmentService.createKeyDateAdjustment(bookingId, request)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sentence adjustment")
data class SentenceAdjustmentResponse(
  @Schema(description = "The sentence adjustment id", required = true)
  val id: Long,
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "The sequence of the sentence within this booking", required = true)
  val sentenceSequence: Long,
  @Schema(description = "Adjustment type", required = true)
  val adjustmentType: SentencingAdjustmentType,
  @Schema(description = "Date adjustment is applied", required = true)
  val adjustmentDate: LocalDate,
  @Schema(description = "Start of the period which contributed to the adjustment", required = false)
  val adjustmentFromDate: LocalDate?,
  @Schema(description = "End of the period which contributed to the adjustment", required = false)
  val adjustmentToDate: LocalDate?,
  @Schema(description = "Number of days for the adjustment", required = true)
  val adjustmentDays: Long,
  @Schema(description = "Comment", required = false)
  val comment: String?,
  @Schema(description = "Flag to indicate if the adjustment is being applied", required = true)
  val active: Boolean,
)

data class SentencingAdjustmentType(
  @Schema(description = "code", required = true, example = "RX")
  val code: String,
  @Schema(description = "description", required = true, example = "Remand")
  val description: String,
)

@Schema(description = "Sentence adjustment")
data class CreateSentenceAdjustmentRequest(
  @Schema(
    description = "NOMIS Adjustment type code from SENTENCE_ADJUSTMENTS",
    required = true,
    example = "RX",
    allowableValues = ["RSR", "UR", "S240A", "RST", "RX"]
  )
  @field:NotBlank
  val adjustmentTypeCode: String = "",
  @Schema(description = "Date adjustment is applied", required = false, defaultValue = "current date")
  val adjustmentDate: LocalDate = LocalDate.now(),
  @Schema(description = "Start of the period which contributed to the adjustment", required = false)
  val adjustmentFromDate: LocalDate?,
  @Schema(description = "Number of days for the adjustment", required = true)
  @field:Min(0)
  val adjustmentDays: Long = -1,
  @Schema(description = "Comment", required = false)
  val comment: String?,
  @Schema(description = "Flag to indicate if the adjustment is being applied", required = false, defaultValue = "true")
  val active: Boolean = true,
)

@Schema(description = "Key date adjustment")
data class CreateKeyDateAdjustmentRequest(
  @Schema(
    description = "NOMIS Adjustment type code from SENTENCE_ADJUSTMENTS",
    required = true,
    example = "ADA",
    allowableValues = ["LAL", "UAL", "RADA", "ADA", "SREM"]
  )
  @field:NotBlank
  val adjustmentTypeCode: String = "",
  @Schema(description = "Date adjustment is applied", required = false, defaultValue = "current date")
  val adjustmentDate: LocalDate = LocalDate.now(),
  @Schema(description = "Start of the period which contributed to the adjustment", required = false)
  val adjustmentFromDate: LocalDate?,
  @Schema(description = "Number of days for the adjustment", required = true)
  @field:Min(0)
  val adjustmentDays: Long = -1,
  @Schema(description = "Comment", required = false)
  val comment: String?,
  @Schema(description = "Flag to indicate if the adjustment is being applied", required = false, defaultValue = "true")
  val active: Boolean = true,
)

@Schema(description = "Create adjustment response")
data class CreateAdjustmentResponse(
  val id: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Key date adjustment")
data class KeyDateAdjustmentResponse(
  @Schema(description = "The key date adjustment id", required = true)
  val id: Long,
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "Adjustment type", required = true)
  val adjustmentType: SentencingAdjustmentType,
  @Schema(description = "Date adjustment is applied", required = true)
  val adjustmentDate: LocalDate,
  @Schema(description = "Start of the period which contributed to the adjustment", required = false)
  val adjustmentFromDate: LocalDate?,
  @Schema(description = "End of the period which contributed to the adjustment", required = false)
  val adjustmentToDate: LocalDate?,
  @Schema(description = "Number of days for the adjustment", required = true)
  val adjustmentDays: Long,
  @Schema(description = "Comment", required = false)
  val comment: String?,
  @Schema(description = "Flag to indicate if the adjustment is being applied", required = true)
  val active: Boolean,
)
