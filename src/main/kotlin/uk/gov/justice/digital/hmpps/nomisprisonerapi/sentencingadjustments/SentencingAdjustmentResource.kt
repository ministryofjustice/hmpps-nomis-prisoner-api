package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencingadjustments

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
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SentencingAdjustmentResource(private val sentencingAdjustmentService: SentencingAdjustmentService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/sentence-adjustments/{adjustmentId}")
  @Operation(
    summary = "get specific sentence adjustment",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves a sentence adjustment by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the sentence adjustment details",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Adjustment not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getSentenceAdjustment(
    @Schema(description = "Sentence adjustment id", example = "12345", required = true)
    @PathVariable
    adjustmentId: Long,
  ): SentenceAdjustmentResponse = sentencingAdjustmentService.getSentenceAdjustment(adjustmentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/sentence-adjustments/{adjustmentId}")
  @Operation(
    summary = "deletes specific sentence adjustment",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Deletes a sentence adjustment by id",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "the sentence adjustment has been deleted",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteSentenceAdjustment(
    @Schema(description = "Sentence adjustment id", example = "12345", required = true)
    @PathVariable
    adjustmentId: Long,
  ): Unit = sentencingAdjustmentService.deleteSentenceAdjustment(adjustmentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/sentence-adjustments/{adjustmentId}")
  @Operation(
    summary = "Updates specific sentence adjustment. The related booking and sentence can not be changed",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Updates a sentence adjustment by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the sentence adjustment has been updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Supplied data is invalid, for instance missing required fields or invalid values. See schema for details",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateSentenceAdjustment(
    @Schema(description = "Sentence adjustment id", example = "12345", required = true)
    @PathVariable
    adjustmentId: Long,
    @RequestBody @Valid
    request: UpdateSentenceAdjustmentRequest,
  ): Unit = sentencingAdjustmentService.updateSentenceAdjustment(adjustmentId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/booking-id/{bookingId}/sentences/{sentenceSequence}/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new sentence adjustment",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Creates a new sentence adjustment (aka Debit/Credit). Key dates will not be recalculated as a side effect of this operation",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateSentenceAdjustmentRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created Sentence adjustment id",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Supplied data is invalid, for instance missing required fields or invalid values. See schema for details",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking or sentence sequence do not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createSentenceAdjustment(
    @Schema(description = "Booking Id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Sentence sequence number", example = "1", required = true)
    @PathVariable
    sentenceSequence: Long,
    @RequestBody @Valid
    request: CreateSentenceAdjustmentRequest,
  ): CreateAdjustmentResponse = sentencingAdjustmentService.createSentenceAdjustment(bookingId, sentenceSequence, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/key-date-adjustments/{adjustmentId}")
  @Operation(
    summary = "get specific key date adjustment",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves a key date adjustment by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the key date adjustment details",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Adjustment not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getKeyDateAdjustment(
    @Schema(description = "Key date adjustment id", example = "12345", required = true)
    @PathVariable
    adjustmentId: Long,
  ): KeyDateAdjustmentResponse = sentencingAdjustmentService.getKeyDateAdjustment(adjustmentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/key-date-adjustments/{adjustmentId}")
  @Operation(
    summary = "Updates specific key date adjustment. The related booking can not be changed",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Updates a sentence adjustment by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the key date adjustment has been updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Supplied data is invalid, for instance missing required fields or invalid values. See schema for details",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Adjustment not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateKeyDateAdjustment(
    @Schema(description = "Key date adjustment id", example = "12345", required = true)
    @PathVariable
    adjustmentId: Long,
    @RequestBody @Valid
    request: UpdateKeyDateAdjustmentRequest,
  ): Unit = sentencingAdjustmentService.updateKeyDateAdjustment(adjustmentId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/key-date-adjustments/{adjustmentId}")
  @Operation(
    summary = "deletes specific key date adjustment",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Deletes a key date adjustment by id",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "the key date adjustment has been deleted",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteKeyDateAdjustment(
    @Schema(description = "Key date adjustment id", example = "12345", required = true)
    @PathVariable
    adjustmentId: Long,
  ): Unit = sentencingAdjustmentService.deleteKeyDateAdjustment(adjustmentId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/booking-id/{bookingId}/adjustments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new key date adjustment",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Creates a new key date adjustment. Key dates will be recalculated as a side effect of this operation",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateKeyDateAdjustmentRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created key date adjustment",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Supplied data is invalid, for instance missing required fields or invalid values. See schema for details",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createKeyDateAdjustment(
    @Schema(description = "Booking Id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @RequestBody @Valid
    request: CreateKeyDateAdjustmentRequest,
  ): CreateAdjustmentResponse = sentencingAdjustmentService.createKeyDateAdjustment(bookingId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/booking-id/{bookingId}/sentencing-adjustments")
  @Operation(
    summary = "get active sentence and key date adjustments for a booking",
    description = "Retrieves all the current active sentence and key date adjustments (by booking) for a prisoner. Requires NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the list of adjustments details",
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
        description = "Forbidden to access this endpoint when role NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getActiveAdjustments(
    @Schema(description = "NOMIS booking Id", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Indicate if should return just active adjustments", required = true)
    @RequestParam(value = "active-only", required = false, defaultValue = "true")
    activeOnly: Boolean,
  ): SentencingAdjustmentsResponse = if (activeOnly) {
    sentencingAdjustmentService.getActiveSentencingAdjustments(bookingId = bookingId)
  } else {
    sentencingAdjustmentService.getAllSentencingAdjustments(bookingId = bookingId)
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sentence adjustment")
data class SentenceAdjustmentResponse(
  @Schema(description = "The sentence adjustment id", required = true)
  val id: Long,
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "The booking sequence", required = true)
  val bookingSequence: Int,
  @Schema(description = "Indicates whether for this booking the prisoner has been released", required = true)
  val hasBeenReleased: Boolean,
  @Schema(description = "Current prison or OUT")
  val prisonId: String,
  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,
  @Schema(description = "The sequence of the sentence within this booking", required = true)
  val sentenceSequence: Long,
  @Schema(description = "Adjustment type", required = true)
  val adjustmentType: SentencingAdjustmentType,
  @Schema(description = "Date adjustment is applied", required = false)
  val adjustmentDate: LocalDate?,
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
  @Schema(description = "Flag to indicate the adjustment is hidden from end users. This is true when it was created as part of a key date adjustment", required = true)
  val hiddenFromUsers: Boolean = false,
)

data class SentencingAdjustmentType(
  @Schema(description = "code", required = true, example = "RX")
  val code: String,
  @Schema(description = "description", required = true, example = "Remand")
  val description: String,
)

@Schema(description = "Sentence adjustment create request")
data class CreateSentenceAdjustmentRequest(
  @Schema(
    description = "NOMIS Adjustment type code from SENTENCE_ADJUSTMENTS",
    required = true,
    example = "RX",
    allowableValues = ["RSR", "UR", "S240A", "RST", "RX"],
  )
  @field:NotBlank
  val adjustmentTypeCode: String = "",
  @Schema(description = "Date adjustment is applied", required = false)
  val adjustmentDate: LocalDate?,
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

@Schema(description = "Sentence adjustment update request")
data class UpdateSentenceAdjustmentRequest(
  @Schema(
    description = "NOMIS Adjustment type code from SENTENCE_ADJUSTMENTS",
    required = true,
    example = "RX",
    allowableValues = ["RSR", "UR", "S240A", "RST", "RX"],
  )
  @field:NotBlank
  val adjustmentTypeCode: String = "",
  @Schema(description = "Date adjustment is applied", required = false)
  val adjustmentDate: LocalDate?,
  @Schema(description = "Start of the period which contributed to the adjustment", required = false)
  val adjustmentFromDate: LocalDate?,
  @Schema(description = "Number of days for the adjustment", required = true)
  @field:Min(0)
  val adjustmentDays: Long = -1,
  @Schema(description = "Comment", required = false)
  val comment: String?,
  @Schema(description = "Flag to indicate if the adjustment is being applied", required = false)
  val active: Boolean?,
  @Schema(description = "Sentence sequence", required = true)
  @field:Min(0)
  val sentenceSequence: Long = -1,
)

@Schema(description = "Key date adjustment create request")
data class CreateKeyDateAdjustmentRequest(
  @Schema(
    description = "NOMIS Adjustment type code from SENTENCE_ADJUSTMENTS",
    required = true,
    example = "ADA",
    allowableValues = ["LAL", "UAL", "RADA", "ADA", "SREM"],
  )
  @field:NotBlank
  val adjustmentTypeCode: String = "",
  @Schema(description = "Date adjustment is applied", required = false)
  val adjustmentDate: LocalDate?,
  @Schema(description = "Start of the period which contributed to the adjustment", required = true)
  val adjustmentFromDate: LocalDate,
  @Schema(description = "Number of days for the adjustment", required = true)
  @field:Min(0)
  val adjustmentDays: Long = -1,
  @Schema(description = "Comment", required = false)
  val comment: String?,
  @Schema(description = "Flag to indicate if the adjustment is being applied", required = false, defaultValue = "true")
  val active: Boolean = true,
)

@Schema(description = "Key date adjustment update request")
data class UpdateKeyDateAdjustmentRequest(
  @Schema(
    description = "NOMIS Adjustment type code from SENTENCE_ADJUSTMENTS",
    required = true,
    example = "ADA",
    allowableValues = ["LAL", "UAL", "RADA", "ADA", "SREM"],
  )
  @field:NotBlank
  val adjustmentTypeCode: String = "",
  @Schema(description = "Date adjustment is applied", required = false)
  val adjustmentDate: LocalDate?,
  @Schema(description = "Start of the period which contributed to the adjustment", required = true)
  val adjustmentFromDate: LocalDate,
  @Schema(description = "Number of days for the adjustment", required = true)
  @field:Min(0)
  val adjustmentDays: Long = -1,
  @Schema(description = "Comment", required = false)
  val comment: String?,
  @Schema(description = "Flag to indicate if the adjustment is being applied", required = false)
  val active: Boolean?,
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
  @Schema(description = "The booking sequence", required = true)
  val bookingSequence: Int,
  @Schema(description = "Indicates whether for this booking the prisoner has been released", required = true)
  val hasBeenReleased: Boolean,
  @Schema(description = "Current prison or OUT")
  val prisonId: String,
  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,
  @Schema(description = "Adjustment type", required = true)
  val adjustmentType: SentencingAdjustmentType,
  @Schema(description = "Date adjustment is applied", required = false)
  val adjustmentDate: LocalDate?,
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

@Schema(description = "Adjustment id")
data class AdjustmentIdResponse(
  @Schema(description = "The adjustment id", required = true)
  val adjustmentId: Long,
  @Schema(description = "SENTENCE or KEY_DATE", required = true)
  val adjustmentCategory: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sentencing adjustment")
data class SentencingAdjustmentsResponse(
  val keyDateAdjustments: List<KeyDateAdjustmentResponse>,
  val sentenceAdjustments: List<SentenceAdjustmentResponse>,
)
