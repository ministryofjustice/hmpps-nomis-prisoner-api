package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/csip"], produces = [MediaType.APPLICATION_JSON_VALUE])
class CSIPResource(private val csipService: CSIPService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
  @GetMapping("/ids")
  @Operation(
    summary = "get csip IDs by filter",
    description = "Retrieves a paged list of csip ids by filter. Requires ROLE_NOMIS_CSIP.",
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
        description = "Forbidden to access this endpoint when role NOMIS_CSIP not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIdsByFilter(
    pageRequest: Pageable,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by those that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by those that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
  ): Page<CSIPIdResponse> =
    csipService.findIdsByFilter(
      pageRequest = pageRequest,
      CSIPFilter(
        toDate = toDate,
        fromDate = fromDate,
      ),
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
  @GetMapping("/{id}")
  @Operation(
    summary = "Get CSIP details",
    description = "Gets csip details. Requires role NOMIS_CSIP",
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
  fun getCSIP(
    @Schema(description = "CSIP id") @PathVariable id: Long,
  ) = csipService.getCSIP(id)

  @PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
  @GetMapping("/count")
  @Operation(
    summary = "Get csip count",
    description = "Gets a count of all csips. Requires role NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
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
    ],
  )
  fun getCSIPCount() = csipService.getCSIPCount()
}

@Schema(description = "CSIP Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CSIPResponse(
  @Schema(description = "The csip id")
  val id: Long,
  @Schema(description = "The offender")
  val offender: Offender,
  @Schema(description = "The booking id associated with the CSIP")
  val bookingId: Long,
  @Schema(description = "The original location when the CSIP was created")
  val originalAgencyLocation: String,

  @Schema(description = "Log number")
  val logNumber: String?,

  @Schema(description = "Date/Time incident occurred")
  val incidentDateTime: LocalDateTime?,
  @Schema(description = "Type of incident")
  val type: CodeDescription,
  @Schema(description = "Location of the incident")
  val location: CodeDescription,

  @Schema(description = "The Area of work, aka function")
  val areaOfWork: CodeDescription,
  @Schema(description = "The person reporting the incident - free text")
  val reportedBy: String?,
  @Schema(description = "Date reported")
  val reportedDate: LocalDate,

  @Schema(description = "proActive Referral")
  val proActiveReferral: Boolean,
  @Schema(description = "If a staff member was assaulted")
  val staffAssaulted: Boolean,
  @Schema(description = "If assaulted, the staff member name")
  val staffAssaultedName: String?,

  @Schema(description = "Additional information for the CSIP Report")
  val reportDetails: ReportDetails,

  @Schema(description = "Safer custody screening")
  val saferCustodyScreening: SaferCustodyScreening,

  @Schema(description = "Investigation details of the incident")
  val investigation: Investigation,

  @Schema(description = "DecisionAndActions")
  val decision: Decision,

  @Schema(description = "Case Manager involved")
  val caseManager: String?,
  @Schema(description = "Reason for plan")
  val planReason: String?,
  @Schema(description = "Date of first review")
  val firstCaseReviewDate: LocalDate?,
  @Schema(description = "CSIP Plans")
  val plans: List<Plan>,

  @Schema(description = "CSIP Reviews")
  val reviews: List<Review>,

  @Schema(description = "Associated CSIP document Ids")
  val documents: List<DocumentIdResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SaferCustodyScreening(
  @Schema(description = "Result of the Safer Custody Screening")
  val outcome: CodeDescription?,
  @Schema(description = "Who recorded the data")
  val recordedBy: String?,
  @Schema(description = "When the the SCS occurred")
  val recordedDate: LocalDate?,
  @Schema(description = "Why the decision was made")
  val reasonForDecision: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReportDetails(
  @Schema(description = "Date the offender is released")
  val releaseDate: LocalDate?,
  @Schema(description = "How the offender was involved")
  val involvement: CodeDescription?,
  @Schema(description = "Concern description")
  val concern: String?,
  @Schema(description = "Contributory factors")
  val factors: List<FactorResponse>,
  @Schema(description = "known reasons for the involvement")
  val knownReasons: String?,
  @Schema(description = "Additional information")
  val otherInformation: String?,

  @Schema(description = "If the safer custody team were informed")
  val saferCustodyTeamInformed: Boolean,
  @Schema(description = "If the referral has been completed")
  val referralComplete: Boolean,
  @Schema(description = "Who completed the referral")
  val referralCompletedBy: String?,
  @Schema(description = "Date the referral was completed")
  val referralCompletedDate: LocalDate?,

)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FactorResponse(
  @Schema(description = "Factor type id")
  val id: Long,
  @Schema(description = "Contributory Factor")
  val type: CodeDescription,
  @Schema(description = "Factor comment")
  val comment: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Investigation(
  @Schema(description = "Staff involved in the incident")
  val staffInvolved: String?,
  @Schema(description = "Whether any evidence was secured")
  val evidenceSecured: String? = null,
  @Schema(description = "Why the incident occurred")
  val reasonOccurred: String? = null,
  @Schema(description = "Normal behaviour of the offender")
  val usualBehaviour: String? = null,
  @Schema(description = "Offender's trigger")
  val trigger: String? = null,
  @Schema(description = "Protective factors")
  val protectiveFactors: String? = null,
  @Schema(description = "Interview")
  val interviews: List<InterviewDetails>?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InterviewDetails(
  @Schema(description = "Person being interviewed")
  val interviewee: String,
  @Schema(description = "date of interview")
  val date: LocalDate,
  @Schema(description = "Why the incident occurred")
  val role: CodeDescription,
  @Schema(description = "Additional data regarding the interview")
  val comments: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Decision(
  @Schema(description = "Conclusion & Reason for decision")
  val conclusion: String?,
  @Schema(description = "Outcome")
  var decisionOutcome: CodeDescription?,
  @Schema(description = "Signed off by")
  var signedOffRole: CodeDescription?,
  @Schema(description = "Recorded By")
  var recordedBy: String?,
  @Schema(description = "Recorded Date")
  var recordedDate: LocalDate?,
  @Schema(description = "What to do next")
  var nextSteps: String?,
  @Schema(description = "Other information to take into consideration")
  var otherDetails: String?,
  @Schema(description = "Action list")
  val actions: Actions,

)
data class Actions(
  val openCSIPAlert: Boolean,
  val nonAssociationsUpdated: Boolean,
  val observationBook: Boolean,
  val unitOrCellMove: Boolean,
  val csraOrRsraReview: Boolean,
  val serviceReferral: Boolean,
  val simReferral: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Plan(
  @Schema(description = "Plan Id")
  val id: Long,
  @Schema(description = "Details of the need")
  val identifiedNeed: String,
  @Schema(description = "Intervention plan")
  val intervention: String,
  @Schema(description = "Information regarding progression of plan")
  val progression: String?,
  @Schema(description = "The person reporting - free text")
  val referredBy: String?,
  @Schema(description = "When created")
  val createdDate: LocalDate,
  @Schema(description = "Target date of plan")
  val targetDate: LocalDate,
  @Schema(description = "Plan closed date")
  val closedDate: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Review(
  @Schema(description = "Review Id")
  val id: Long,
  @Schema(description = "Sequence number")
  val reviewSequence: Int,
  @Schema(description = "Summary details")
  val attendees: List<Attendee>,
  @Schema(description = "Summary details")
  val remainOnCSIP: Boolean,
  @Schema(description = "Summary details")
  val csipUpdated: Boolean,
  @Schema(description = "Summary details")
  val caseNote: Boolean,
  @Schema(description = "Summary details")
  val closeCSIP: Boolean,
  @Schema(description = "Summary details")
  val peopleInformed: Boolean,
  @Schema(description = "Summary details")
  val summary: String?,
  @Schema(description = "Next Review date")
  val nextReviewDate: LocalDate?,
  @Schema(description = "Review closed date")
  val closeDate: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Attendee(
  @Schema(description = "Review Attendee/Contributor Id")
  val id: Long,
  @Schema(description = "Name of attendee/contributor")
  val name: String?,
  @Schema(name = "Role of attendee/contributor")
  val role: String?,
  @Schema(name = "If attended (otherwise contributor)")
  val attended: Boolean,
  @Schema(name = "Contribution")
  val contribution: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Staff(
  @Schema(description = "Username of first account related to staff")
  val username: String,
  @Schema(description = "NOMIS staff id")
  val staffId: Long,
  @Schema(description = "First name of staff member")
  val firstName: String,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Offender(
  @Schema(description = "NOMIS id")
  val offenderNo: String,
  @Schema(description = "First name of offender")
  val firstName: String?,
  @Schema(description = "Last name of offender")
  val lastName: String,
)
