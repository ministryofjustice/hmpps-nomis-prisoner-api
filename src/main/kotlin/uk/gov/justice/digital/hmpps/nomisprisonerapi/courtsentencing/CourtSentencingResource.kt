package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CourtSentencingResource(private val courtSentencingService: CourtSentencingService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-cases/{id}")
  @Operation(
    summary = "get a court case",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves a court case by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the court case details",
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
        description = "Court case not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourtCase(
    @Schema(description = "Court case id", example = "12345")
    @PathVariable
    id: Long,
    @Schema(description = "Offender No", example = "12345")
    @PathVariable
    offenderNo: String,
  ): CourtCaseResponse = courtSentencingService.getCourtCase(id, offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/court-cases/{id}")
  @Operation(
    summary = "get a court case, without offenderNo validation",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves a court case by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the court case details",
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
        description = "Court case not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourtCaseForMigration(
    @Schema(description = "Court case id", example = "12345")
    @PathVariable
    id: Long,
  ): CourtCaseResponse = courtSentencingService.getCourtCaseForMigration(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-cases")
  @Operation(
    summary = "get court cases for an offender",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves a court case by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the list of court cases",
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
        description = "Offender not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourtCasesByOffender(
    @Schema(description = "Offender No", example = "AA12345")
    @PathVariable
    offenderNo: String,
  ): List<CourtCaseResponse> = courtSentencingService.getCourtCasesByOffender(offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases/get-list")
  @Tag(name = "Multiple Court case lookup")
  @Operation(
    summary = "Retrieves a specified list of court cases for an offender",
    description = "Requires role <b>NOMIS_PRISONER_API__SYNCHRONISATION__RW</b>",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          array = ArraySchema(schema = Schema(implementation = Long::class)),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "List of Court case responses returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = CourtCaseResponse::class)),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request is invalid, see response for details",
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
  suspend fun getCourtCasesByOffender(
    @Schema(description = "Offender No", example = "AA12345")
    @PathVariable
    offenderNo: String,
    @org.springframework.web.bind.annotation.RequestBody nomisCaseIds: List<Long>,
  ): List<CourtCaseResponse> = courtSentencingService.getCourtCases(offenderNo = offenderNo, idList = nomisCaseIds)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-cases/ids")
  @Operation(
    summary = "get court case ids for an offender",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves all court case ids by offender",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the list of court case ids",
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
        description = "Offender not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourtCaseIdsByOffender(
    @Schema(description = "Offender No", example = "AA12345")
    @PathVariable
    offenderNo: String,
  ): List<Long> = courtSentencingService.findCourtCaseIdsByOffender(offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-cases/post-merge")
  @Operation(
    summary = "Get court cases affected by the last prisoner merge of two prisoner records",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. The court cases returned - if any -  includes cases that may have been cloned onto the active booking and those deactivated",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the lists of court cases affected. Not all merges result in cases being changed, so these lists might be empty even if the current booking has a court case",
      ),
      ApiResponse(
        responseCode = "400",
        description = "This prisoner has no merge recorded",
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
        description = "Offender not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourtCasesChangedByMergePrisoners(
    @Schema(description = "Offender No", example = "AA12345")
    @PathVariable
    offenderNo: String,
  ): PostPrisonerMergeCaseChanges = courtSentencingService.getCourtCasesChangedByMergePrisoners(offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @DeleteMapping("/prisoners/{offenderNo}/sentencing/court-cases/{id}")
  @Operation(
    summary = "delete a court case",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Deletes a court case by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "court case deleted",
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
  fun deleteCourtCase(
    @Schema(description = "Court case id", example = "12345")
    @PathVariable
    id: Long,
    @Schema(description = "Offender No", example = "AB2134")
    @PathVariable
    offenderNo: String,
  ) = courtSentencingService.deleteCourtCase(caseId = id, offenderNo = offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sequence}")
  @Operation(
    summary = "get sentences for an offender using the given case.booking id and sentence sequence",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves a court case by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the sentence details",
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
        description = "Sentence not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender booking not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getOffenderSentence(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "4565456", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Sentence sequence", example = "1", required = true)
    @PathVariable
    sequence: Long,
  ): SentenceResponse = courtSentencingService.getOffenderSentence(
    offenderNo = offenderNo,
    caseId = caseId,
    sentenceSequence = sequence,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/booking-id/{bookingId}/sentences/recall")
  @Operation(
    summary = "get all active recall sentences for a booking",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves all active recall sentences for a booking",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the active recall sentences",
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
        description = "Offender booking not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getActiveRecallSentences(
    @Schema(description = "Booking ID", example = "12345", required = true)
    @PathVariable
    bookingId: Long,
  ): List<SentenceResponse> = courtSentencingService.getActiveRecallSentencesByBookingId(bookingId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/sentence-terms/booking-id/{bookingId}/sentence-sequence/{sentenceSequence}/term-sequence/{termSequence}")
  @Operation(
    summary = "get a sentence term by id (offender booking, sentence sequence and term sequence",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves a sentence term by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the sentence term details",
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
        description = "Sentence not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender booking not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getOffenderSentenceTerm(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "offender booking id", example = "4565456", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Sentence sequence", example = "1", required = true)
    @PathVariable
    sentenceSequence: Long,
    @Schema(description = "term sequence", example = "1", required = true)
    @PathVariable
    termSequence: Long,
  ): SentenceTermResponse = courtSentencingService.getOffenderSentenceTerm(
    offenderNo = offenderNo,
    offenderBookingId = bookingId,
    sentenceSequence = sentenceSequence,
    termSequence = termSequence,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/court-cases/{caseId}/sentences")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Sentence",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Creates a new Sentence for the offender booking associated with the court case",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateSentenceRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created Sentence",
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
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createSentence(
    @Schema(description = "Offender number", example = "AB1234K", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "4565456", required = true)
    @PathVariable
    caseId: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: CreateSentenceRequest,
  ): CreateSentenceResponse = courtSentencingService.createSentence(offenderNo, caseId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sentenceSequence}/sentence-terms")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Sentence term",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Creates a new sentence term for the specified sentence",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = SentenceTermRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created Sentence Term",
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
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createSentenceTerm(
    @Schema(description = "Offender number", example = "AB1234K", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "4565456", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Sentence sequence", example = "4565456", required = true)
    @PathVariable
    sentenceSequence: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: SentenceTermRequest,
  ): CreateSentenceTermResponse = courtSentencingService.createSentenceTerm(offenderNo, caseId, sentenceSequence = sentenceSequence, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates Sentence",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Updates a Sentence for the offender and case",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateSentenceRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Sentence updated",
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
      ApiResponse(
        responseCode = "404",
        description = "Sentence does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateSentence(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "4565456", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Sentence sequence", example = "1", required = true)
    @PathVariable
    sequence: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: CreateSentenceRequest,
  ) = courtSentencingService.updateSentence(
    sentenceSequence = sequence,
    caseId = caseId,
    offenderNo = offenderNo,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/sentences/recall")
  @Operation(
    summary = "Recalls Sentences by convert the specified sentences to the requested recall sentence",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Recalls sentences for the offender",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = ConvertToRecallRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Sentences converted to recall",
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
      ApiResponse(
        responseCode = "404",
        description = "One or more sentence does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun convertToRecallSentences(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: ConvertToRecallRequest,
  ) = courtSentencingService.convertToRecallSentences(
    offenderNo = offenderNo,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/prisoners/{offenderNo}/sentences/recall")
  @Operation(
    summary = "Updates Recalls Sentences",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Recalls sentences for the offender",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = UpdateRecallRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Recall Sentences updated",
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
      ApiResponse(
        responseCode = "404",
        description = "One or more sentence does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateRecallSentences(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: UpdateRecallRequest,
  ) = courtSentencingService.updateRecallSentences(
    offenderNo = offenderNo,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/prisoners/{offenderNo}/sentences/recall/restore-original")
  @Operation(
    summary = "Deletes Recalls Sentences and replaces with original sentence",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW replaces recall sentences for the offender",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = DeleteRecallRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Recall Sentences deleted and replaced with original sentences",
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
      ApiResponse(
        responseCode = "404",
        description = "One or more sentence does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteRecallSentences(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: DeleteRecallRequest,
  ) = courtSentencingService.replaceRecallSentences(
    offenderNo = offenderNo,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/prisoners/{offenderNo}/sentences/recall/restore-previous")
  @Operation(
    summary = "Deletes Recalls Sentences and replaces with previous recall sentence",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW replaces recall sentences for the offender",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = RevertRecallRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Recall Sentences deleted and replaced with previous recall sentences",
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
      ApiResponse(
        responseCode = "404",
        description = "One or more sentence does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun revertRecallSentences(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: RevertRecallRequest,
  ) = courtSentencingService.revertRecallSentences(
    offenderNo = offenderNo,
    request = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sentenceSequence}/sentence-terms/{termSequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates Sentence Term",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Updates a Sentence Term for the offender",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateSentenceRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Sentence updated",
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
      ApiResponse(
        responseCode = "404",
        description = "Sentence term does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateSentenceTerm(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "4565456", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Sentence sequence", example = "1", required = true)
    @PathVariable
    sentenceSequence: Long,
    @Schema(description = "term sequence", example = "1", required = true)
    @PathVariable
    termSequence: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: SentenceTermRequest,
  ) = courtSentencingService.updateSentenceTerm(
    sentenceSequence = sentenceSequence,
    termSequence = termSequence,
    caseId = caseId,
    offenderNo = offenderNo,
    termRequest = request,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sequence}")
  @Operation(
    summary = "deletes a specific sentence",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Deletes a sentence by case.booking and sentence sequence",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "the sentence has been deleted",
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
  fun deleteSentence(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "4565456", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Sentence sequence", example = "1", required = true)
    @PathVariable
    sequence: Long,
  ): Unit = courtSentencingService.deleteSentence(offenderNo, caseId, sequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/prisoners/{offenderNo}/court-cases/{caseId}/sentences/{sentenceSequence}/sentence-terms/{termSequence}")
  @Operation(
    summary = "deletes a specific sentence",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Deletes a sentence by case.booking, sentence sequence and term sequence",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "the sentence term has been deleted",
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
  fun deleteSentenceTerm(
    @Schema(description = "Offender no", example = "AA668EC", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "4565456", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Sentence sequence", example = "1", required = true)
    @PathVariable
    sentenceSequence: Long,
    @Schema(description = "Term sequence", example = "1", required = true)
    @PathVariable
    termSequence: Long,
  ): Unit = courtSentencingService.deleteSentenceTerm(
    offenderNo = offenderNo,
    caseId = caseId,
    sentenceSequence = sentenceSequence,
    termSequence = termSequence,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Court Case",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Creates a new Court Case for the offender and latest booking",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateCourtCaseRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created Court case",
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
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createCourtCase(
    @Schema(description = "Offender No", example = "AK1234B", required = true)
    @PathVariable
    offenderNo: String,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: CreateCourtCaseRequest,
  ): CreateCourtCaseResponse = courtSentencingService.createCourtCase(offenderNo, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases/{id}/repair")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Creates a new Court Case",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Replaces existing court case for the offender. ",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtCaseRepairRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Repaired Court case",
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
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun repairCourtCase(
    @Schema(description = "case ID", example = "1234", required = true)
    @PathVariable
    id: Long,
    @Schema(description = "Offender No", example = "AK1234B", required = true)
    @PathVariable
    offenderNo: String,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: CourtCaseRepairRequest,
  ): CourtCaseRepairResponse = courtSentencingService.courtCaseRepair(id, offenderNo, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases/clone/{caseId}")
  @Operation(
    summary = "Clones court cases from the cases' booking to the current booking",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW. The court cases along with dependent cases and all child elements including adjustments are copied to the current booking",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Created Court cases",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Case id supplied is already the latest booking or doesn't belong to the prisoner",
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
        description = "Case does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun cloneCourtCasesToLatestBooking(
    @Schema(description = "Offender number", example = "A1234KT", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case id", example = "1233", required = true)
    @PathVariable
    caseId: Long,
  ): BookingCourtCaseCloneResponse = courtSentencingService.cloneCourtCasesToLatestBookingFor(
    offenderNo = offenderNo,
    caseId = caseId,
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/court-appearances")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Court Appearance",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Creates a new Court Appearance for the offender and given Court Case",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtAppearanceRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created Court Appearance",
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
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court case does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createCourtAppearance(
    @Schema(description = "Offender no", example = "AB1234A", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "34565", required = true)
    @PathVariable
    caseId: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: CourtAppearanceRequest,
  ): CreateCourtAppearanceResponse = courtSentencingService.createCourtAppearance(offenderNo, caseId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/charges")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Offender Charge",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Creates a new Offender Charge for the offender and latest booking. Will not associate with a Court Event",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = OffenderChargeRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Created Charge",
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
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createCourtCharge(
    @Schema(description = "Offender No", example = "AK1234B", required = true)
    @PathVariable
    offenderNo: String,
    @PathVariable
    caseId: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: OffenderChargeRequest,
  ): OffenderChargeIdResponse = courtSentencingService.createCourtCharge(offenderNo, caseId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/court-appearances/{eventId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates Court Appearance",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Updates a new Court Appearance for the offender and given Court Case",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtAppearanceRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Court Appearance updated",
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
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court case does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court appearance does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateCourtAppearance(
    @Schema(description = "Offender no", example = "AB1234A", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "34565", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Case appearance Id", example = "34565", required = true)
    @PathVariable
    eventId: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: CourtAppearanceRequest,
  ): UpdateCourtAppearanceResponse = courtSentencingService.updateCourtAppearance(offenderNo, caseId, eventId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @DeleteMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/court-appearances/{eventId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Deletes Court Appearance",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Deletes s Court Appearance for the offender.",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CourtAppearanceRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Court Appearance deleted",
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
  fun deleteCourtAppearance(
    @Schema(description = "Offender no", example = "AB1234A", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "34565", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Case appearance Id", example = "34565", required = true)
    @PathVariable
    eventId: Long,
  ) = courtSentencingService.deleteCourtAppearance(offenderNo, caseId, eventId)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PutMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/court-appearances/{courtEventId}/charges/{chargeId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates Charge",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Updates a Court Event Charge for the offender and given Appearance and Court Case (latest booking)",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = OffenderChargeRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Offender Charge updated",
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
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court case does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateCharge(
    @Schema(description = "Offender no", example = "AB1234A", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "34565", required = true)
    @PathVariable
    caseId: Long,
    @Schema(description = "Charge Id", example = "34565", required = true)
    @PathVariable
    chargeId: Long,
    @Schema(description = "Court event Id", example = "34565", required = true)
    @PathVariable
    courtEventId: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: OffenderChargeRequest,
  ) = courtSentencingService.updateCourtCharge(offenderNo, caseId, chargeId, courtEventId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-appearances/{id}")
  @Operation(
    summary = "get a court appearance",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves a court appearance by id",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the court appearance details",
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
        description = "Court appearance not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourtAppearance(
    @Schema(description = "Court appearance id", example = "12345")
    @PathVariable
    id: Long,
    @Schema(description = "Offender No", example = "12345")
    @PathVariable
    offenderNo: String,
  ): CourtEventResponse = courtSentencingService.getCourtAppearance(id, offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/offender-charges/{offenderChargeId}")
  @Operation(
    summary = "get an offender charge",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves offender charge details. Offender Charges are at the booking level.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the court appearance details",
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
        description = "Offender charge not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getOffenderCharge(
    @Schema(description = "Offender Charge id", example = "12345")
    @PathVariable
    offenderChargeId: Long,
    @Schema(description = "Offender No", example = "12345")
    @PathVariable
    offenderNo: String,
  ): OffenderChargeResponse = courtSentencingService.getOffenderCharge(offenderChargeId, offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-appearances/{eventId}/charges/{chargeId}")
  @Operation(
    summary = "get the court event charge",
    description = "Requires role NOMIS_PRISONER_API__SYNCHRONISATION__RW. Retrieves the court event charge ",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "the court appearance details",
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
        description = "Court event charge not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Court Appearance not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourtEventCharge(
    @Schema(description = "Offender Charge id", example = "12345")
    @PathVariable
    chargeId: Long,
    @Schema(description = "Event id", example = "12345")
    @PathVariable
    eventId: Long,
    @Schema(description = "Offender No", example = "AB12345")
    @PathVariable
    offenderNo: String,
  ): CourtEventChargeResponse = courtSentencingService.getCourtEventCharge(chargeId = chargeId, eventId = eventId, offenderNo = offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @GetMapping("/court-cases/ids")
  @Operation(
    summary = "get court case IDs by filter",
    description = "Retrieves a paged list of court case ids by filter. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of ids are returned",
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
        description = "Forbidden to access this endpoint when role ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCourtCaseIdsByFilter(
    @PageableDefault(size = 20)
    @ParameterObject
    pageRequest: Pageable,
    @RequestParam(value = "fromDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by court cases that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by court cases that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
  ): Page<CourtCaseIdResponse> = courtSentencingService.findCourtCaseIdsByFilter(
    pageRequest = pageRequest,
    CourtCaseFilter(
      toDateTime = toDate?.plusDays(1)?.atStartOfDay(),
      fromDateTime = fromDate?.atStartOfDay(),
    ),
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/case-identifiers")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Refreshes the list of Case identifiers associated with the case",
    description = "Required role NOMIS_PRISONER_API__SYNCHRONISATION__RW Refreshes the list of Case identifiers associated with the case (identifier type CASE/INFO#)",
    requestBody = RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CaseIdentifierRequest::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Case Identifiers Refreshed",
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
        description = "Court case does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun refreshCaseIdentifiers(
    @Schema(description = "Offender no", example = "AB1234A", required = true)
    @PathVariable
    offenderNo: String,
    @Schema(description = "Case Id", example = "34565", required = true)
    @PathVariable
    caseId: Long,
    @org.springframework.web.bind.annotation.RequestBody @Valid
    request: CaseIdentifierRequest,
  ) = courtSentencingService.refreshCaseIdentifiers(offenderNo, caseId, request)
}

@Schema(description = "Court Case")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtCaseResponse(
  val id: Long,
  val offenderNo: String,
  val bookingId: Long,
  val primaryCaseInfoNumber: String?,
  val caseSequence: Int,
  val caseStatus: CodeDescription,
  val legalCaseType: CodeDescription,
  val beginDate: LocalDate?,
  val courtId: String,
  val combinedCaseId: Long?,
  val sourceCombinedCaseIds: List<Long>,
  val statusUpdateStaffId: Long?,
  val statusUpdateDate: LocalDate?,
  val statusUpdateComment: String?,
  val statusUpdateReason: String?,
  val lidsCaseId: Int?,
  val lidsCombinedCaseId: Int?,
  val createdDateTime: LocalDateTime,
  val createdByUsername: String,
  val modifiedByUsername: String?,
  val courtEvents: List<CourtEventResponse>,
  val offenderCharges: List<OffenderChargeResponse>,
  val caseInfoNumbers: List<CaseIdentifierResponse>,
  val sentences: List<SentenceResponse>,
)

@Schema(description = "Court Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtEventResponse(
  val id: Long,
  val caseId: Long?,
  val offenderNo: String,
  val eventDateTime: LocalDateTime,
  val courtEventType: CodeDescription,
  val eventStatus: CodeDescription,
  val directionCode: CodeDescription?,
  val judgeName: String?,
  val courtId: String,
  val outcomeReasonCode: OffenceResultCodeResponse?,
  val commentText: String?,
  val orderRequestedFlag: Boolean?,
  val holdFlag: Boolean?,
  val nextEventRequestFlag: Boolean?,
  val nextEventDateTime: LocalDateTime?,
  val createdDateTime: LocalDateTime,
  val createdByUsername: String,
  val modifiedByUsername: String?,
  val courtEventCharges: List<CourtEventChargeResponse>,
  val courtOrders: List<CourtOrderResponse>,
)

@Schema(description = "Offender Charge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderChargeResponse(
  val id: Long,
  val offence: OffenceResponse,
  val offencesCount: Int?,
  val offenceDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val plea: CodeDescription?,
  val propertyValue: BigDecimal?,
  val totalPropertyValue: BigDecimal?,
  val cjitCode1: String?,
  val cjitCode2: String?,
  val cjitCode3: String?,
  // derived field. OffenceResultCodeResponse contains the reference data version of charge status
  val chargeStatus: CodeDescription?,
  val resultCode1: OffenceResultCodeResponse?,
  val resultCode2: OffenceResultCodeResponse?,
  val mostSeriousFlag: Boolean,
  val lidsOffenceNumber: Int?,
  val createdByUsername: String,
  val modifiedByUsername: String?,
)

@Schema(description = "Offence Result Code")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenceResultCodeResponse(
  val chargeStatus: String,
  val code: String,
  val description: String,
  val dispositionCode: String,
  val conviction: Boolean,
)

@Schema(description = "Offence")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenceResponse(
  val offenceCode: String,
  val statuteCode: String,
  val description: String,
)

@Schema(description = "Court Event Charge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtEventChargeResponse(
  val eventId: Long,
  val offenderCharge: OffenderChargeResponse,
  val offencesCount: Int?,
  val offenceDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val plea: CodeDescription?,
  val propertyValue: BigDecimal?,
  val totalPropertyValue: BigDecimal?,
  val cjitCode1: String?,
  val cjitCode2: String?,
  val cjitCode3: String?,
  val resultCode1: OffenceResultCodeResponse?,
  val resultCode2: OffenceResultCodeResponse?,
  val mostSeriousFlag: Boolean,
  val linkedCaseDetails: LinkedCaseChargeDetails?,
  val createdByUsername: String,
  val modifiedByUsername: String?,
)

@Schema(description = "Linked case details for a court event charge")
data class LinkedCaseChargeDetails(
  @Schema(description = "Source caseId")
  val caseId: Long,
  @Schema(description = "Target court eventId")
  val eventId: Long,
  val dateLinked: LocalDate,
)

@Schema(description = "Court Order")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtOrderResponse(
  val id: Long,
  val eventId: Long,
  val courtDate: LocalDate,
  val issuingCourt: String,
  val courtInfoId: String?,
  val orderType: String,
  val orderStatus: String,
  val dueDate: LocalDate?,
  val requestDate: LocalDate?,
  val seriousnessLevel: CodeDescription?,
  val commentText: String?,
  val nonReportFlag: Boolean?,
  val sentencePurposes: List<SentencePurposeResponse>,
)

@Schema(description = "Sentence Purpose")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SentencePurposeResponse(
  val orderId: Long,
  val orderPartyCode: String,
  val purposeCode: String,
)

@Schema(description = "Offender Sentence")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SentenceResponse(
  val bookingId: Long,
  val sentenceSeq: Long,
  val status: String,
  val calculationType: CodeDescription,
  val category: CodeDescription,
  val startDate: LocalDate,
  val courtOrder: CourtOrderResponse?,
  val consecSequence: Int?,
  val endDate: LocalDate?,
  val commentText: String?,
  val absenceCount: Int?,
  val caseId: Long?,
  val etdCalculatedDate: LocalDate?,
  val mtdCalculatedDate: LocalDate?,
  val ltdCalculatedDate: LocalDate?,
  val ardCalculatedDate: LocalDate?,
  val crdCalculatedDate: LocalDate?,
  val pedCalculatedDate: LocalDate?,
  val npdCalculatedDate: LocalDate?,
  val ledCalculatedDate: LocalDate?,
  val sedCalculatedDate: LocalDate?,
  val prrdCalculatedDate: LocalDate?,
  val tariffCalculatedDate: LocalDate?,
  val dprrdCalculatedDate: LocalDate?,
  val tusedCalculatedDate: LocalDate?,
  val aggSentenceSequence: Int?,
  val aggAdjustDays: Int?,
  val sentenceLevel: String?,
  val extendedDays: Int?,
  val counts: Int?,
  val statusUpdateReason: String?,
  val statusUpdateComment: String?,
  val statusUpdateDate: LocalDate?,
  val statusUpdateStaffId: Long?,
  val fineAmount: BigDecimal?,
  val dischargeDate: LocalDate?,
  val nomSentDetailRef: Long?,
  val nomConsToSentDetailRef: Long?,
  val nomConsFromSentDetailRef: Long?,
  val nomConsWithSentDetailRef: Long?,
  val lineSequence: Int?,
  val hdcExclusionFlag: Boolean?,
  val hdcExclusionReason: String?,
  val cjaAct: String?,
  val sled2Calc: LocalDate?,
  val startDate2Calc: LocalDate?,
  val createdDateTime: LocalDateTime,
  val createdByUsername: String,
  val modifiedByUsername: String?,
  val sentenceTerms: List<SentenceTermResponse>,
  val offenderCharges: List<OffenderChargeResponse>,
  val missingCourtOffenderChargeIds: List<Long>,
  val prisonId: String,
  val recallCustodyDate: RecallCustodyDate?,
)

@Schema(description = "Recall custody return date data")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RecallCustodyDate(
  val returnToCustodyDate: LocalDate,
  val recallLength: Long,
  val comments: String?,
)

@Schema(description = "Sentence Term")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SentenceTermResponse(
  val termSequence: Long,
  val sentenceTermType: CodeDescription?,
  val years: Int?,
  val months: Int?,
  val weeks: Int?,
  val days: Int?,
  val hours: Int?,
  val startDate: LocalDate,
  val endDate: LocalDate?,
  val lifeSentenceFlag: Boolean,
  val prisonId: String,
  val createdByUsername: String,
  val modifiedByUsername: String?,
)

@Schema(description = "Court case create request")
data class CreateCourtCaseRequest(
  // the warrant date in sentencing
  val startDate: LocalDate,
  // either A for Adult or sentences to create new type
  val legalCaseType: String,
  // Court Id (establishment) not in new service will need to use (one of ) appearance values
  val courtId: String,
  // ACTIVE, INACTIVE, CLOSED
  val status: String,
  // the prototype implies only 1 appearance can be associated with the case on creation
  val courtAppearance: CourtAppearanceRequest? = null,
  // optional case reference (dps holds case ref at the appearance level - max of one when creating a case)
  val caseReference: String? = null,

    /* not currently provided by sentencing service:
    caseSequence: Int,
    caseStatus: String,
    combinedCase: CourtCase?,
    statusUpdateReason: String?,
    statusUpdateComment: String?,
    statusUpdateDate: LocalDate?,
    statusUpdateStaff: Staff?,
    lidsCaseId: Int?,
    lidsCaseNumber: Int,
    lidsCombinedCaseId: Int?
     */
)

@Schema(description = "Court case repair request")
data class CourtCaseRepairRequest(
  val startDate: LocalDate,
  val legalCaseType: String,
  val courtId: String,
  val status: String,
  val courtAppearances: List<CourtAppearanceRepairRequest> = emptyList(),
  val offenderCharges: List<OffenderChargeRepairRequest> = emptyList(),
  val caseReferences: CaseIdentifierRequest? = null,
)

@Schema(description = "Court Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtAppearanceRepairRequest(
  val eventDateTime: LocalDateTime,
  val courtEventType: String,
  val courtId: String,
  val outcomeReasonCode: String?,
  val nextEventDateTime: LocalDateTime?,
  val courtEventCharges: List<CourtEventChargeRepairRequest>,
  val nextCourtId: String?,
)

@Schema(description = "Court Charge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderChargeRepairRequest(
  val id: String,
  val offenceCode: String,
  val offenceDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val resultCode1: String?,
)

@Schema(description = "Court Charge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtEventChargeRepairRequest(
  val id: String,
  val offenceDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val resultCode1: String?,
)

@Schema(description = "Create court case response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCourtCaseResponse(
  val id: Long,
  // TODO remove this as not used
  val courtAppearanceIds: List<CreateCourtAppearanceResponse> = listOf(),
)

@Schema(description = "Court case repair response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtCaseRepairResponse(
  val caseId: Long,
  val bookingId: Long,
  val courtAppearanceIds: List<Long> = listOf(),
  val offenderChargeIds: List<Long> = listOf(),
)

@Schema(description = "court case id")
data class CourtCaseIdResponse(
  @Schema(description = "Court case Id")
  val caseId: Long,
)

@Schema(description = "Create sentence response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateSentenceResponse(
  val sentenceSeq: Long,
  val bookingId: Long,
)

@Schema(description = "Create sentence term response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateSentenceTermResponse(
  val sentenceSeq: Long,
  val termSeq: Long,
  val bookingId: Long,
)

@Schema(description = "Create adjustment response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCourtAppearanceResponse(
  val id: Long,
  val courtEventChargesIds: List<OffenderChargeIdResponse> = listOf(),
  @Schema(description = "Result of a clone court case operation when the appearance is added to a previous booking. Else null")
  val clonedCourtCases: BookingCourtCaseCloneResponse?,
)

@Schema(description = "Create adjustment response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateCourtAppearanceResponse(
  val createdCourtEventChargesIds: List<OffenderChargeIdResponse> = listOf(),
  val deletedOffenderChargesIds: List<OffenderChargeIdResponse> = listOf(),
)

@Schema(description = "Create offender charge response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderChargeIdResponse(
  val offenderChargeId: Long,
)

@Schema(description = "Court Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtAppearanceRequest(
  val eventDateTime: LocalDateTime,
  val courtEventType: String,
  val courtId: String,
  val outcomeReasonCode: String?,
  val nextEventDateTime: LocalDateTime?,
  // update requests will also determine the offences to remove from the appearance
  val courtEventCharges: List<Long>,
  // nomis UI doesn't allow this during a create but DPS does
  val nextCourtId: String?,

    /* not currently provided by sentencing service:
    val commentText: String?, no sign in new service
    val orderRequestedFlag: Boolean?,
    val holdFlag: Boolean?,
    val directionCode: CodeDescription?,
    val judgeName: String?,
     */
)

@Schema(description = "Court Charge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderChargeRequest(
  val offenceCode: String,
  val offenceDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val resultCode1: String?,

    /*
    val plea: String?,
    val propertyValue: BigDecimal?,
    val offencesCount: Long?,
    val totalPropertyValue: BigDecimal?,
    val cjitCode1: String?,
    val cjitCode2: String?,
    val cjitCode3: String?,
    val chargeStatus: String?,
    val resultCode2: String?,  // DPS data model has 1 outcome
    val resultCode1Indicator: String?,
    val resultCode2Indicator: String?,
    val lidsOffenceNumber: Int?,
     */

  /* mostSeriousFlag has been removed - DPS not providing */
)

@Schema(description = "Court Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExistingOffenderChargeRequest(
  val offenderChargeId: Long,
  val offenceCode: String,
  val offenceDate: LocalDate?,
  val offenceEndDate: LocalDate?,
  val resultCode1: String?,
)

@Schema(description = "Sentence request")
data class CreateSentenceRequest(
  val startDate: LocalDate,
  val endDate: LocalDate? = null,
  // either I or A
  val status: String = "A",
  // 1967, 1991, 2003, 2020
  val sentenceCategory: String,
  // eg ADIMP_ORA
  val sentenceCalcType: String,
  // 'IND' or 'AGG'
  val sentenceLevel: String,
  val fine: BigDecimal? = null,
  val offenderChargeIds: List<Long>,
  val consecutiveToSentenceSeq: Long? = null,
  val eventId: Long,
)

data class SentenceId(
  val offenderBookingId: Long,
  val sentenceSequence: Long,
)

@Schema(description = "Return to custody data")
data class ReturnToCustodyRequest(
  val returnToCustodyDate: LocalDate,
  val enteredByStaffUsername: String,
  val recallLength: Int,
)

@Schema(description = "Recall convert request")
data class ConvertToRecallRequest(
  val sentences: List<RecallRelatedSentenceDetails>,
  val returnToCustody: ReturnToCustodyRequest? = null,
  val recallRevocationDate: LocalDate,
)

@Schema(description = "Recall convert request")
data class UpdateRecallRequest(
  val sentences: List<RecallRelatedSentenceDetails>,
  val returnToCustody: ReturnToCustodyRequest? = null,
  val recallRevocationDate: LocalDate,
  @Schema(description = "the breach court appearance that require updating")
  val beachCourtEventIds: List<Long>,
)

@Schema(description = "Recall revert request when a recall is replaced with older recall")
data class RevertRecallRequest(
  val sentences: List<RecallRelatedSentenceDetails>,
  val returnToCustody: ReturnToCustodyRequest? = null,
  @Schema(description = "the breach court appearance that require deleting")
  val beachCourtEventIds: List<Long>,
)

@Schema(description = "Recall convert response")
data class ConvertToRecallResponse(
  @Schema(description = "the breach court appearance ids created")
  val courtEventIds: List<Long>,
  @Schema(description = "the sentence adjustments and parent sentence that have been activate by the recall")
  val sentenceAdjustmentsActivated: List<SentenceIdAndAdjustmentIds>,
  @Schema(description = "Result of a clone court case operation when the recall sentences is added to a previous booking. Else null")
  val clonedCourtCases: BookingCourtCaseCloneResponse?,
)

@Schema(description = "Recall convert response")
data class SentenceIdAndAdjustmentIds(
  val sentenceId: SentenceId,
  val adjustmentIds: List<Long>,
)

@Schema(description = "Delete recall sentence request")
data class DeleteRecallRequest(
  val sentences: List<RecallRelatedSentenceDetails>,
  @Schema(description = "the breach court appearance that require deleting")
  val beachCourtEventIds: List<Long>,
)

@Schema(description = "Recall sentences to set")
data class RecallRelatedSentenceDetails(
  val sentenceId: SentenceId,
  val sentenceCategory: String,
  val sentenceCalcType: String,
  val active: Boolean = true,
)

@Schema(description = "Sentence term request")
data class SentenceTermRequest(
  val years: Int? = null,
  val months: Int? = null,
  val weeks: Int? = null,
  val days: Int? = null,
  val hours: Int? = null,
  val sentenceTermType: String,
  val lifeSentenceFlag: Boolean = false,
)

@Schema(description = "Court case associated reference")
data class CaseIdentifier(
  val reference: String,
  val createdDate: LocalDateTime,
)

@Schema(description = "Case identifier list")
data class CaseIdentifierRequest(
  val caseIdentifiers: List<CaseIdentifier>,
)

@Schema(description = "Case related reference")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseIdentifierResponse(
  @Schema(description = "The type of case identifier", example = "CASE/INFO#")
  val type: String,
  @Schema(description = "The value of the case identifier", example = "asd/123")
  val reference: String,
  @Schema(description = "The time the case identifier was created", example = "2020-07-17T12:34:56")
  val createDateTime: LocalDateTime,
  @Schema(description = "The time the case identifier was last changed", example = "2021-07-16T12:34:56")
  val modifiedDateTime: LocalDateTime?,
  @Schema(
    description = "The name of the module that last changed it, indicates if this was NOMIS or the synchronisation service",
    example = "DPS_SYNCHRONISATION",
  )
  val auditModuleName: String?,
)

@Schema(description = "Court Cases (and related charges and sentences) created or updated due to the latest prisoner merge")
data class PostPrisonerMergeCaseChanges(
  @Schema(description = "Court cases and related child entities create due to the merge after being copied from a previous booking")
  val courtCasesCreated: List<CourtCaseResponse> = emptyList(),
  @Schema(description = "Court cases and related child entities deactivated due to the merge after being cloned from a previous booking")
  val courtCasesDeactivated: List<CourtCaseResponse> = emptyList(),
)

@Schema(description = "Court Cases created due to a booking clone operation")
data class ClonedCourtCaseResponse(
  @Schema(description = "Created court case and children")
  val courtCase: CourtCaseResponse,
  @Schema(description = "Source court case and children that the cases were cloned from")
  val sourceCourtCase: CourtCaseResponse,
)

@Schema(description = "Response for Court Cases booking clone operations")
data class BookingCourtCaseCloneResponse(
  @Schema(description = "Court Cases created")
  val courtCases: List<ClonedCourtCaseResponse>,
  @Schema(description = "Sentence adjustments created")
  val sentenceAdjustments: List<SentenceIdAndAdjustmentsCreated>,
)

data class SentenceIdAndAdjustmentsCreated(
  val sentenceId: SentenceId,
  val adjustmentIds: List<Long>,
)
