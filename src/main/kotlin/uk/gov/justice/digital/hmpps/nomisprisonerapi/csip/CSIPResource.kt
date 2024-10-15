package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
class CSIPResource(private val csipService: CSIPService) {

  @PutMapping("/csip")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Creates or updates a csip",
    description = "Creates or updates a csip report and its children. Requires ROLE_NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP Updated or created",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = UpsertCSIPResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "One or more fields in the request contains invalid data",
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
  fun upsertCSIP(@RequestBody @Valid request: UpsertCSIPRequest) = csipService.upsertCSIP(request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CSIP')")
  @GetMapping("/prisoners/{offenderNo}/csip/to-migrate")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets csips for an offender",
    description = "Retrieves csips for a prisoner from all bookings. Requires ROLE_NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "CSIPs Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = PrisonerCSIPsResponse::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CSIP",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist or has no csips",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCSIPsToMigrate(
    @Schema(description = "Offender No AKA prisoner number", example = "A1234AK")
    @PathVariable
    offenderNo: String,
  ): PrisonerCSIPsResponse = csipService.getCSIPs(offenderNo)

  @GetMapping("/csip/ids")
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

  @GetMapping("/csip/{id}")
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
    @RequestParam(value = "includeDocumentIds", required = false)
    includeDocumentIds: Boolean = false,
  ) = csipService.getCSIP(id, includeDocumentIds)

  @DeleteMapping("/csip/{csipId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a csip report",
    description = "Deletes a csip report. Requires ROLE_NOMIS_CSIP",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Csip report Deleted",
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
  fun deleteCSIP(
    @Schema(description = "CSIP Factor Id", example = "12345")
    @PathVariable
    csipId: Long,
  ): Unit = csipService.deleteCSIP(csipId)

  @GetMapping("/csip/count")
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

@Schema(description = "The list of CSIPs held against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerCSIPsResponse(
  val offenderCSIPs: List<CSIPResponse>,
)

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
  val originalAgencyId: String?,

  @Schema(description = "Log number")
  val logNumber: String?,

  @Schema(description = "Date/Time incident occurred")
  val incidentDate: LocalDate,
  @Schema(description = "Date/Time incident occurred")
  val incidentTime: LocalTime?,
  @Schema(description = "Type of incident")
  val type: CodeDescription,
  @Schema(description = "Location of the incident")
  val location: CodeDescription,

  @Schema(description = "The Area of work, aka function")
  val areaOfWork: CodeDescription,
  @Schema(description = "The person reporting the incident - free text")
  val reportedBy: String,
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
  val investigation: InvestigationDetails,

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
  val documents: List<DocumentIdResponse>? = null,

  @Schema(description = "The date and time the report was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the report")
  val createdBy: String,
  @Schema(description = "Real name of the person who created the report")
  val createdByDisplayName: String?,
  @Schema(description = "The date and time the report was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the report")
  val lastModifiedBy: String?,
  @Schema(description = "Real name of the person who last updated the report")
  val lastModifiedByDisplayName: String?,
)

@Schema(description = "A response after a csip has been upserted in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpsertCSIPResponse(
  @Schema(description = "The nomis csip id")
  val nomisCSIPReportId: Long,

  @Schema(description = "The prisoner nomis Id relating to this csip")
  val offenderNo: String,

  @Schema(description = "Any new CSIP components that were created")
  val components: List<CSIPComponent>,
)
data class CSIPComponent(
  @Schema(description = "The child component created")
  val component: Component,
  @Schema(description = "The nomisId of the created component")
  val nomisId: Long,
  @Schema(description = "The dpsId of the created component")
  val dpsId: String,
) {
  enum class Component {
    ATTENDEE,
    FACTOR,
    INTERVIEW,
    PLAN,
    REVIEW,
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SaferCustodyScreening(
  @Schema(description = "Result of the Safer Custody Screening")
  val outcome: CodeDescription?,
  @Schema(description = "The username of the person who recorded the data")
  val recordedBy: String?,
  @Schema(description = "Real name of who recorded the data")
  val recordedByDisplayName: String?,
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
  val factors: List<CSIPFactorResponse>,
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
  @Schema(description = "Real name of the person who completed the referral")
  val referralCompletedByDisplayName: String?,
  @Schema(description = "Date the referral was completed")
  val referralCompletedDate: LocalDate?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvestigationDetails(
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
  @Schema(description = "Interview Id")
  val id: Long,
  @Schema(description = "Person being interviewed")
  val interviewee: String,
  @Schema(description = "date of interview")
  val date: LocalDate,
  @Schema(description = "Why the incident occurred")
  val role: CodeDescription,
  @Schema(description = "Additional data regarding the interview")
  val comments: String? = null,

  @Schema(description = "The date and time the interview was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the interview")
  val createdBy: String,
  @Schema(description = "Real name of the person who created the interview")
  val createdByDisplayName: String?,
  @Schema(description = "The date and time the interview was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the interview")
  val lastModifiedBy: String?,
  @Schema(description = "Real name of the person who last updated the interview")
  val lastModifiedByDisplayName: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Decision(
  @Schema(description = "Conclusion & Reason for decision")
  val conclusion: String?,
  @Schema(description = "Outcome")
  var decisionOutcome: CodeDescription?,
  @Schema(description = "Signed off by")
  var signedOffRole: CodeDescription?,
  @Schema(description = "The username of the person who recorded the decision")
  var recordedBy: String?,
  @Schema(description = "Real name of who recorded the decision")
  var recordedByDisplayName: String?,
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
data class CSIPFactorResponse(
  @Schema(description = "Factor type id")
  val id: Long,
  @Schema(description = "Contributory Factor")
  val type: CodeDescription,
  @Schema(description = "Factor comment")
  val comment: String?,

  @Schema(description = "The date and time the factor was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the factor")
  val createdBy: String,
  @Schema(description = "Real name of the person who created the factor")
  val createdByDisplayName: String?,
  @Schema(description = "The date and time the factor was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the factor")
  val lastModifiedBy: String?,
  @Schema(description = "Real name of the person who last updated the factor")
  val lastModifiedByDisplayName: String?,

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

  @Schema(description = "The date and time the plan was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the plan")
  val createdBy: String,
  @Schema(description = "Real name of the person who created the plan")
  val createdByDisplayName: String?,
  @Schema(description = "The date and time the plan was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the plan")
  val lastModifiedBy: String?,
  @Schema(description = "Real name of the person who last updated the plan")
  val lastModifiedByDisplayName: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Review(
  @Schema(description = "Review Id")
  val id: Long,
  @Schema(description = "Sequence number")
  val reviewSequence: Int,
  @Schema(description = "Attendees to the review")
  val attendees: List<Attendee>,
  @Schema(description = "Whether to remain on CSIP")
  val remainOnCSIP: Boolean,
  @Schema(description = "If the csip has been updated")
  val csipUpdated: Boolean,
  @Schema(description = "If a case note was added")
  val caseNote: Boolean,
  @Schema(description = "If the csip is closed")
  val closeCSIP: Boolean,
  @Schema(description = "Whether people were informed")
  val peopleInformed: Boolean,
  @Schema(description = "Summary details")
  val summary: String?,
  @Schema(description = "Next Review date")
  val nextReviewDate: LocalDate?,
  @Schema(description = "Review closed date")
  val closeDate: LocalDate?,
  @Schema(description = "The date the review was created")
  val recordedDate: LocalDate,
  @Schema(description = "The username of the person who recorded the review")
  val recordedBy: String,
  @Schema(description = "Real name of who recorded the review")
  val recordedByDisplayName: String?,

  @Schema(description = "The date and time the review was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the review")
  val createdBy: String,
  @Schema(description = "Real name of the person who created the plan")
  val createdByDisplayName: String?,
  @Schema(description = "The date and time the review was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the review")
  val lastModifiedBy: String?,
  @Schema(description = "Real name of the person who last updated the review")
  val lastModifiedByDisplayName: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Attendee(
  @Schema(description = "Review Attendee/Contributor Id")
  val id: Long,
  @Schema(description = "Name of attendee/contributor")
  val name: String?,
  @Schema(description = "Role of attendee/contributor")
  val role: String?,
  @Schema(description = "If attended (otherwise contributor)")
  val attended: Boolean,
  @Schema(description = "Contribution")
  val contribution: String? = null,

  @Schema(description = "The date and time the attendee was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the attendee")
  val createdBy: String,
  @Schema(description = "Real name of the person who created the attendee")
  val createdByDisplayName: String?,
  @Schema(description = "The date and time the attendee was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the attendee")
  val lastModifiedBy: String?,
  @Schema(description = "Real name of the person who last updated the attendee")
  val lastModifiedByDisplayName: String?,
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
