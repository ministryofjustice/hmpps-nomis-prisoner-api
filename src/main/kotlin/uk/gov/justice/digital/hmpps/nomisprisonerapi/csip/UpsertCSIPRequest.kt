package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP Report create/update request")
data class UpsertCSIPRequest(

  @Schema(description = "The csip id", example = "1234")
  val id: Long? = null,
  @Schema(description = "The offender No", example = "A11235BC", required = true)
  val offenderNo: String,
  @Schema(description = "Log number")
  val logNumber: String? = null,

  @Schema(description = "Originating Prison Id")
  val prisonCodeWhenRecorded: String,

  @Schema(description = "Date/Time incident occurred", example = "2023-04-03", required = true)
  val incidentDate: LocalDate,
  @Schema(description = "Date/Time incident occurred", example = "10:00")
  val incidentTime: LocalTime? = null,

  @Schema(description = "Type of incident")
  val typeCode: String,
  @Schema(description = "Location of the incident")
  val locationCode: String,
  @Schema(description = "The Area of work, aka function")
  val areaOfWorkCode: String,

  @Schema(description = "The person reporting the incident - free text")
  val reportedBy: String,
  @Schema(description = "Date reported")
  val reportedDate: LocalDate,

  @Schema(description = "proActive Referral")
  val proActiveReferral: Boolean = false,
  @Schema(description = "If a staff member was assaulted")
  val staffAssaulted: Boolean = false,
  @Schema(description = "If assaulted, the staff member name")
  val staffAssaultedName: String? = null,

  @Schema(description = "Audit detail info")
  val auditDetails: AuditDetailsRequest,

  @Schema(description = "Additional information for the CSIP Report")
  val reportDetailRequest: UpsertReportDetailsRequest? = null,

  @Schema(description = "Safer custody screening")
  val saferCustodyScreening: SaferCustodyScreeningRequest? = null,

  @Schema(description = "Investigation details of the incident")
  val investigation: InvestigationDetailRequest? = null,

  @Schema(description = "DecisionAndActions")
  val decision: DecisionRequest? = null,

  @Schema(description = "Case Manager involved")
  val caseManager: String? = null,
  @Schema(description = "Reason for plan")
  val planReason: String? = null,
  @Schema(description = "Date of first review")
  val firstCaseReviewDate: LocalDate? = null,
  @Schema(description = "CSIP Plans")
  val plans: List<PlanRequest>? = null,

  @Schema(description = "CSIP Reviews")
  val reviews: List<ReviewRequest>? = null,
)

data class UpsertReportDetailsRequest(
  @Schema(description = "How the offender was involved")
  val involvementCode: String? = null,
  @Schema(description = "Concern description")
  val concern: String? = null,

  @Schema(description = "known reasons for the involvement")
  val knownReasons: String? = null,
  @Schema(description = "Additional information")
  val otherInformation: String? = null,

  @Schema(description = "If the safer custody team were informed")
  val saferCustodyTeamInformed: Boolean = false,
  @Schema(description = "If the referral has been completed")
  val referralComplete: Boolean = false,
  @Schema(description = "Who completed the referral")
  val referralCompletedBy: String? = null,
  @Schema(description = "Real name of the person who completed the referral")
  val referralCompletedByDisplayName: String? = null,
  @Schema(description = "Date the referral was completed")
  val referralCompletedDate: LocalDate? = null,

  @Schema(description = "Contributory factors")
  val factors: List<CSIPFactorRequest>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SaferCustodyScreeningRequest(
  @Schema(description = "Result of the Safer Custody Screening")
  val scsOutcomeCode: String,
  @Schema(description = "The username of the person who recorded the data")
  val recordedBy: String,
  // @Schema(description = "Real name of who recorded the data")
  // val recordedByDisplayName: String,
  @Schema(description = "When the the SCS occurred")
  val recordedDate: LocalDate,
  @Schema(description = "Why the decision was made")
  val reasonForDecision: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CSIPFactorRequest(
  @Schema(description = "Factor type id")
  val id: Long? = null,
  @Schema(description = "Contributory Factor")
  val typeCode: String,
  @Schema(description = "Factor comment")
  val comment: String?,

  @Schema(description = "Audit information for the CSIP Factor")
  val auditDetails: AuditDetailsRequest,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvestigationDetailRequest(
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
  val interviews: List<InterviewDetailRequest>?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InterviewDetailRequest(
  // @Schema(description = "Interview Id")
  // val id: Long,
  @Schema(description = "Person being interviewed")
  val interviewee: String,
  @Schema(description = "date of interview")
  val date: LocalDate,
  @Schema(description = "Why the incident occurred")
  val roleCode: String,
  @Schema(description = "Additional data regarding the interview")
  val comments: String? = null,

  @Schema(description = "Audit information for the Interview")
  val auditDetails: AuditDetailsRequest,

)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DecisionRequest(
  @Schema(description = "Conclusion & Reason for decision")
  val conclusion: String?,
  @Schema(description = "Outcome")
  var decisionOutcomeCode: String?,
  @Schema(description = "Signed off by")
  var signedOffRoleCode: String?,
  @Schema(description = "The username of the person who recorded the decision")
  var recordedBy: String?,
  // @Schema(description = "Real name of who recorded the decision")
  // var recordedByDisplayName: String?,
  @Schema(description = "Recorded Date")
  var recordedDate: LocalDate?,
  @Schema(description = "What to do next")
  var nextSteps: String?,
  @Schema(description = "Other information to take into consideration")
  var otherDetails: String?,
  @Schema(description = "Action list")
  val actions: ActionsRequest,
)

data class ActionsRequest(
  val openCSIPAlert: Boolean,
  val nonAssociationsUpdated: Boolean,
  val observationBook: Boolean,
  val unitOrCellMove: Boolean,
  val csraOrRsraReview: Boolean,
  val serviceReferral: Boolean,
  val simReferral: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PlanRequest(
  @Schema(description = "Plan Id")
  val id: Long? = null,
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

  @Schema(description = "Audit information for the CSIP Plan")
  val auditDetails: AuditDetailsRequest,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReviewRequest(
  @Schema(description = "Review Id")
  val id: Long? = null,
  // @Schema(description = "Sequence number")
  // val reviewSequence: Int,
  @Schema(description = "Attendees to the review")
  val attendees: List<AttendeeRequest>,
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
  // @Schema(description = "Real name of who recorded the review")
  // val recordedByDisplayName: String?,

  @Schema(description = "Audit information for the CSIP Review")
  val auditDetails: AuditDetailsRequest,

)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AttendeeRequest(
  // @Schema(description = "Review Attendee/Contributor Id")
  // val id: Long,
  @Schema(description = "Name of attendee/contributor")
  val name: String?,
  @Schema(description = "Role of attendee/contributor")
  val role: String?,
  @Schema(description = "If attended (otherwise contributor)")
  val attended: Boolean,
  @Schema(description = "Contribution")
  val contribution: String? = null,

  @Schema(description = "Audit information for the CSIP Attendee")
  val auditDetails: AuditDetailsRequest,
)

@Schema(description = "Common Audit request details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuditDetailsRequest(
  @Schema(description = "Date time record was created")
  override var createDatetime: LocalDateTime,
  @Schema(description = "Username of person that created the record (might also be a system) ")
  override var createUsername: String,
//  @Schema(description = "Real name of person that created the record (might by null for system users)")
  // override var createdByDisplayName: String?,

  @Schema(description = "Username of person that last modified the record (might also be a system)")
  override var modifyUserId: String? = null,
//  @Schema(description = "Real name of person that modified the record (might by null for system users)")
//  override var modifyDisplayName: String?,
  @Schema(description = "Date time record was last modified")
  override var modifyDatetime: LocalDateTime? = null,
) : AuditDetails

interface AuditDetails {
  var createDatetime: LocalDateTime
  var createUsername: String
  // var createdByDisplayName: String?

  var modifyDatetime: LocalDateTime?
  var modifyUserId: String?
//  var modifyDisplayName: String?
}
