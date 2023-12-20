package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

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
    @Schema(description = "Booking Id", example = "12345", required = true)
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: CreateCourtCaseRequest,
  ): CreateCourtCaseResponse =
    sentencingService.createCourtCase(offenderNo, request)
}

@Schema(description = "Court Case")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtCaseResponse(
  val id: Long,
  val offenderNo: String,
  val bookingId: Long,
  val caseInfoNumber: String?,
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
)

@Schema(description = "Court Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtEventResponse(
  val id: Long,
  val offenderNo: String,
  val eventDate: LocalDate,
  val startTime: LocalDateTime,
  val courtEventType: CodeDescription,
  val eventStatus: CodeDescription,
  val directionCode: CodeDescription?,
  val judgeName: String?,
  val courtId: String,
  val outcomeReasonCode: String?,
  val commentText: String?,
  val orderRequestedFlag: Boolean?,
  val holdFlag: Boolean?,
  val nextEventRequestFlag: Boolean?,
  val nextEventDate: LocalDate?,
  val nextEventStartTime: LocalDateTime?,
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
  val offenderChargeId: Long,
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
  val caseId: Long?, // may return object ?
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
  val startDate: LocalDate, // the warrant date in sentencing
  val legalCaseType: String, // either A for Adult or sentences to create new type
  val courtId: String, // Court Id (establishment) not in new service will need to use (one of ) appearance values
  val status: String, // ACTIVE, INACTIVE, CLOSED
  val courtAppearances: List<CourtAppearanceRequest> = listOf(),

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

@Schema(description = "Create adjustment response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCourtCaseResponse(
  val id: Long,
  val courtAppearanceIds: List<CreateCourtAppearanceResponse> = listOf(),
)

@Schema(description = "Create adjustment response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCourtAppearanceResponse(
  val id: Long,
)

@Schema(description = "Court Event")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtAppearanceRequest(
  val eventDate: LocalDate,
  val startTime: LocalDateTime, // not in new service (but next event start time is)
  val courtEventType: String,
  val eventStatus: String,
  val courtId: String, // Court Id (agy_loc_id)
  val outcomeReasonCode: String?,
  val nextEventRequestFlag: Boolean?, // will store "to be fixed" from new service if dates not known
  val nextEventDate: LocalDate?,
  val nextEventStartTime: LocalDateTime?,
// val courtEventCharges: List<CourtEventChargeResponse>,
// val courtOrders: List<CourtOrderResponse>,

  /* not currently provided by sentencing service:
  val commentText: String?, no sign in new service
  val orderRequestedFlag: Boolean?,
  val holdFlag: Boolean?,
  val directionCode: CodeDescription?,
  val judgeName: String?,
   */
)
