package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.RequestBody
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
class SentencingResource(private val sentencingService: SentencingService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-cases/{id}")
  @Operation(
    summary = "get a court case",
    description = "Requires role NOMIS_SENTENCING. Retrieves a court case by id",
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
  ): CourtCaseResponse = sentencingService.getCourtCase(id, offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/court-cases/{id}")
  @Operation(
    summary = "get a court case, migration version without offenderNo validation",
    description = "Requires role NOMIS_SENTENCING. Retrieves a court case by id",
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
  ): CourtCaseResponse = sentencingService.getCourtCaseForMigration(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-cases")
  @Operation(
    summary = "get court cases for an offender",
    description = "Requires role NOMIS_SENTENCING. Retrieves a court case by id",
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
  ): List<CourtCaseResponse> = sentencingService.getCourtCasesByOffender(offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/prisoners/booking-id/{bookingId}/sentencing/court-cases")
  @Operation(
    summary = "get court cases for an offender booking",
    description = "Requires role NOMIS_SENTENCING. Retrieves a court case by id",
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
  fun getCourtCasesByOffenderBooking(
    @Schema(description = "Booking Id", example = "12345")
    @PathVariable
    bookingId: Long,
  ): List<CourtCaseResponse> = sentencingService.getCourtCasesByOffenderBooking(bookingId)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/prisoners/booking-id/{bookingId}/sentencing/sentence-sequence/{sequence}")
  @Operation(
    summary = "get sentences for an offender using the given booking id and sentence sequence",
    description = "Requires role NOMIS_SENTENCING. Retrieves a court case by id",
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
    @Schema(description = "Sentence sequence", example = "1")
    @PathVariable
    sequence: Long,
    @Schema(description = "Offender Booking Id", example = "12345")
    @PathVariable
    bookingId: Long,
  ): SentenceResponse = sentencingService.getOffenderSentence(sequence, bookingId)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PostMapping("/prisoners/{offenderNo}/sentencing")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Sentence",
    description = "Required role NOMIS_SENTENCING Creates a new Sentence for the offender and latest booking",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
    @RequestBody @Valid
    request: CreateSentenceRequest,
  ): CreateSentenceResponse =
    sentencingService.createSentence(offenderNo, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PutMapping("/prisoners/booking-id/{bookingId}/sentencing/sentence-sequence/{sequence}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates Sentence",
    description = "Required role NOMIS_SENTENCING Updates a Sentence for the offender and latest booking",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
    @Schema(description = "Booking Id", example = "4565456", required = true)
    @PathVariable
    bookingId: Long,
    @Schema(description = "Sentence sequence", example = "1", required = true)
    @PathVariable
    sequence: Long,
    @RequestBody @Valid
    request: CreateSentenceRequest,
  ) =
    sentencingService.updateSentence(sentenceSequence = sequence, bookingId = bookingId, request = request)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/prisoners/booking-id/{bookingId}/sentencing/sentence-sequence/{sequence}")
  @Operation(
    summary = "deletes a specific sentence",
    description = "Requires role NOMIS_SENTENCING. Deletes a sentence by booking and sentence sequence",
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
    @Schema(description = "Sentence sequence", example = "1")
    @PathVariable
    sequence: Long,
    @Schema(description = "Offender Booking Id", example = "12345")
    @PathVariable
    bookingId: Long,
  ): Unit = sentencingService.deleteSentence(bookingId, sequence)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Court Case",
    description = "Required role NOMIS_SENTENCING Creates a new Court Case for the offender and latest booking",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
    @RequestBody @Valid
    request: CreateCourtCaseRequest,
  ): CreateCourtCaseResponse =
    request.courtAppearance?.let { sentencingService.createCourtCaseHierachy(offenderNo, request) }
      ?: sentencingService.createCourtCase(offenderNo, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/court-appearances")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a new Court Appearance",
    description = "Required role NOMIS_SENTENCING Creates a new Court Appearance for the offender,latest booking and given Court Case",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
    @RequestBody @Valid
    request: CourtAppearanceRequest,
  ): CreateCourtAppearanceResponse =
    sentencingService.createCourtAppearance(offenderNo, caseId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PutMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/court-appearances/{eventId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Updates Court Appearance",
    description = "Required role NOMIS_SENTENCING Updates a new Court Appearance for the offender,latest booking and given Court Case",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
    @RequestBody @Valid
    request: CourtAppearanceRequest,
  ): UpdateCourtAppearanceResponse =
    sentencingService.updateCourtAppearance(offenderNo, caseId, eventId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-appearances/{id}")
  @Operation(
    summary = "get a court appearance",
    description = "Requires role NOMIS_SENTENCING. Retrieves a court appearance by id",
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
  ): CourtEventResponse = sentencingService.getCourtAppearance(id, offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/offender-charges/{offenderChargeId}")
  @Operation(
    summary = "get an offender charge",
    description = "Requires role NOMIS_SENTENCING. Retrieves offender charge details. Offender Charges are at the booking level.",
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
  ): OffenderChargeResponse = sentencingService.getOffenderCharge(offenderChargeId, offenderNo)

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/court-cases/ids")
  @Operation(
    summary = "get court case IDs by filter",
    description = "Retrieves a paged list of court case ids by filter. Requires ROLE_NOMIS_SENTENCING.",
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
        description = "Forbidden to access this endpoint when role ROLE_NOMIS_SENTENCING not present",
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
  ): Page<CourtCaseIdResponse> =
    sentencingService.findCourtCaseIdsByFilter(
      pageRequest = pageRequest,
      CourtCaseFilter(
        toDateTime = toDate?.plusDays(1)?.atStartOfDay(),
        fromDateTime = fromDate?.atStartOfDay(),
      ),
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @PostMapping("/prisoners/{offenderNo}/sentencing/court-cases/{caseId}/case-identifiers")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Refreshes the list of Case identifiers associated with the case",
    description = "Required role NOMIS_SENTENCING Refreshes the list of Case identifiers associated with the case (identifier type CASE/INFO#)",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
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
        description = "Forbidden to access this endpoint when role NOMIS_SENTENCING not present",
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
    @RequestBody @Valid
    request: CaseIdentifierRequest,
  ) = sentencingService.refreshCaseIdentifiers(offenderNo, caseId, request)
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
  val statusUpdateStaffId: Long?,
  val statusUpdateDate: LocalDate?,
  val statusUpdateComment: String?,
  val statusUpdateReason: String?,
  val lidsCaseId: Int?,
  val lidsCombinedCaseId: Int?,
  val lidsCaseNumber: Int,
  val createdDateTime: LocalDateTime,
  val createdByUsername: String,
  val courtEvents: List<CourtEventResponse>,
  val offenderCharges: List<OffenderChargeResponse>,
  val caseInfoNumbers: List<CaseIdentifierResponse>,
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
  val outcomeReasonCode: CodeDescription?,
  val commentText: String?,
  val orderRequestedFlag: Boolean?,
  val holdFlag: Boolean?,
  val nextEventRequestFlag: Boolean?,
  val nextEventDateTime: LocalDateTime?,
  val createdDateTime: LocalDateTime,
  val createdByUsername: String,
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
  val chargeStatus: CodeDescription?,
  val resultCode1: CodeDescription?,
  val resultCode2: CodeDescription?,
  val resultCode1Indicator: String?,
  val resultCode2Indicator: String?,
  val mostSeriousFlag: Boolean,
  val lidsOffenceNumber: Int?,
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
  val resultCode1: CodeDescription?,
  val resultCode2: CodeDescription?,
  val resultCode1Indicator: String?,
  val resultCode2Indicator: String?,
  val mostSeriousFlag: Boolean,
)

@Schema(description = "Court Order")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtOrderResponse(
  val id: Long,
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
  val calculationType: String,
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
  val sentenceTerms: List<SentenceTermResponse>,
  val offenderCharges: List<OffenderChargeResponse>,
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
  val lifeSentenceFlag: Boolean?,
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

@Schema(description = "Create court case response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCourtCaseResponse(
  val id: Long,
  val courtAppearanceIds: List<CreateCourtAppearanceResponse> = listOf(),
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
  val termSeq: Long,
)

@Schema(description = "Create adjustment response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCourtAppearanceResponse(
  val id: Long,
  val courtEventChargesIds: List<CreateCourtEventChargesResponse> = listOf(),
)

@Schema(description = "Create adjustment response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateCourtAppearanceResponse(
  val createdCourtEventChargesIds: List<CreateCourtEventChargesResponse> = listOf(),
  val deletedOffenderChargesIds: List<CreateCourtEventChargesResponse> = listOf(),
)

@Schema(description = "Create adjustment response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCourtEventChargesResponse(
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
  val courtEventChargesToUpdate: List<ExistingOffenderChargeRequest>,
  val courtEventChargesToCreate: List<OffenderChargeRequest>,
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

@Schema(description = "Court Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCourtAppearanceRequest(
  val courtAppearance: CourtAppearanceRequest,
  val existingOffenderChargeIds: List<Long>,
)

@Schema(description = "Court Event")
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
  // the prototype implies only 1 appearance can be associated with the case on creation
  val sentenceTerm: SentenceTermRequest,
  // TODO will we always have an associated court case? nullable for now
  val caseId: Long? = null,
  val offenderChargeIds: List<Long>,
)

@Schema(description = "Sentence term request")
data class SentenceTermRequest(
  val startDate: LocalDate,
  val endDate: LocalDate? = null,
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
  @Schema(description = "The name of the module that last changed it, indicates if this was NOMIS or the synchronisation service", example = "DPS_SYNCHRONISATION")
  val auditModuleName: String?,
)
