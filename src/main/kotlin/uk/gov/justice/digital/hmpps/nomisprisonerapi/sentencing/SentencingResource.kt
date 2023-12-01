package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.fasterxml.jackson.annotation.JsonInclude
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class SentencingResource(private val sentencingService: SentencingService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_SENTENCING')")
  @GetMapping("/prisoners/{offenderNo}/sentencing/court-case/{id}")
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
}

@Schema(description = "Court Case")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CourtCaseResponse(
  val id: Long,
  val offenderNo: String,
  val caseInfoNumber: String?,
  val caseSequence: Int,
  val caseStatus: CodeDescription,
  val caseType: CodeDescription,
  val beginDate: LocalDate?,
  val prisonId: String,
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
  val prisonId: String,
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
