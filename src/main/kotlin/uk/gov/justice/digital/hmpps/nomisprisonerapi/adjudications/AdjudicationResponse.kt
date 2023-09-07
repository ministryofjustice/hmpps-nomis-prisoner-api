package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Adjudication Information")
data class AdjudicationResponse(

  @Schema(
    description = "The adjudication/party sequence, part of the composite key with adjudicationIncidentId",
    required = true,
  )
  val adjudicationSequence: Int,

  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,

  @Schema(description = "The id of the booking associated with the adjudication", required = true)
  val bookingId: Long,

  @Schema(description = "The adjudication number (business key)")
  val adjudicationNumber: Long,

  @Schema(description = "Gender recorded in NOMIS")
  val gender: CodeDescription,

  @Schema(description = "Current prison or null if OUT")
  val currentPrison: CodeDescription?,

  @Schema(description = "Date Prisoner was added to the adjudication ????", required = true)
  val partyAddedDate: LocalDate,

  @Schema(description = "Adjudication comments")
  val comment: String? = null,

  @Schema(description = "Associated incident details")
  val incident: AdjudicationIncident,

  @Schema(description = "Charges associated with this adjudication")
  val charges: List<AdjudicationCharge>,

  @Schema(description = "Investigator that gathers evidence. Used in NOMIS in a small percentage of cases")
  val investigations: List<Investigation>,

  @Schema(description = "hearings associated with this adjudication")
  val hearings: List<Hearing>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdjudicationCharge(
  val offence: AdjudicationOffence,
  val evidence: String?,
  val reportDetail: String?,
  val offenceId: String?,
  val chargeSequence: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdjudicationOffence(
  val code: String,
  val description: String,
  val type: CodeDescription? = null,
  val category: CodeDescription? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdjudicationIncident(
  @Schema(
    description = "The adjudication incident Id, part of the composite key with adjudicationSequence",
    required = true,
  )
  val adjudicationIncidentId: Long,

  @Schema(description = "Reporting staff member", required = true)
  val reportingStaff: Staff,

  @Schema(description = "Date of the associated incident", required = true)
  val incidentDate: LocalDate,

  @Schema(description = "Date and time of the associated incident", required = true)
  val incidentTime: LocalTime,

  @Schema(description = "Date when the associated incident was reported", required = true)
  val reportedDate: LocalDate,

  @Schema(description = "Date and time when the associated incident was reported", required = true)
  val reportedTime: LocalTime,

  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,

  @Schema(description = "Date time when the record was created in NOMIS", required = true)
  val createdDateTime: LocalDateTime,

  @Schema(description = "location where incident took place", required = true)
  val internalLocation: InternalLocation,

  @Schema(description = "Incident type ", required = true)
  val incidentType: CodeDescription,

  @Schema(description = "Incident details")
  val details: String? = null,

  @Schema(description = "Prison where the incident took place")
  val prison: CodeDescription,

  @Schema(description = "Prisoners that witnessed the incident. Rarely used in NOMIS")
  val prisonerWitnesses: List<Prisoner>,

  @Schema(description = "Prisoners that were victims in the incident. Not often used in NOMIS")
  val prisonerVictims: List<Prisoner>,

  @Schema(description = "Other suspects involved in the incident that may or may not have been placed on report")
  val otherPrisonersInvolved: List<Prisoner>,

  @Schema(description = "The officer who reported the incident who may differ from the reporting officer. Often used in NOMIS")
  val reportingOfficers: List<Staff>,

  @Schema(description = "Staff that witnessed the incident. Used in NOMIS in a small percentage of cases")
  val staffWitnesses: List<Staff>,

  @Schema(description = "Staff that was a victim in the incident. Rarely used in NOMIS")
  val staffVictims: List<Staff>,

  @Schema(description = "Other staff that was involved in the incident either using force or some other link. Used in NOMIS in a small percentage of cases")
  val otherStaffInvolved: List<Staff>,

  @Schema(description = "The repairs required due to the damage")
  val repairs: List<Repair>,
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
  @Schema(description = "Username of person who created the record in NOMIS where this staff is used")
  val createdByUsername: String? = null,
  @Schema(description = "date added in NOMIS to the adjudication incident")
  val dateAddedToIncident: LocalDate? = null,
  @Schema(description = "comment about why they were added to the adjudication incident")
  val comment: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Prisoner(
  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,
  @Schema(description = "First name of prisoner")
  val firstName: String?,
  @Schema(description = "Last name of prisoner")
  val lastName: String,
  @Schema(description = "Username of person who created the record in NOMIS where this prisoner is used", required = true)
  val createdByUsername: String,
  @Schema(description = "date added in NOMIS to the adjudication incident")
  val dateAddedToIncident: LocalDate,
  @Schema(description = "comment about why they were added to the adjudication incident")
  val comment: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class InternalLocation(
  @Schema(description = "NOMIS location id")
  val locationId: Long,
  @Schema(description = "NOMIS location code")
  val code: String,
  @Schema(description = "NOMIS location description")
  val description: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Repair(
  val type: CodeDescription,
  val comment: String?,
  val cost: BigDecimal?,
  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Investigation(
  val investigator: Staff,
  val comment: String?,
  val dateAssigned: LocalDate,
  val evidence: List<Evidence>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Evidence(
  val type: CodeDescription,
  val date: LocalDate,
  val detail: String,
  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Hearing(
  val hearingId: Long,
  val type: CodeDescription?,
  @Schema(description = "Hearing scheduled date as set by DPS but not used by NOMIS or set in NOMIS")
  val scheduleDate: LocalDate?,
  @Schema(description = "Hearing scheduled time as set by DPS but not used by NOMIS or set in NOMIS")
  val scheduleTime: LocalTime?,
  @Schema(description = "Hearing date")
  val hearingDate: LocalDate?,
  @Schema(description = "Hearing time")
  val hearingTime: LocalTime?,
  val comment: String?,
  val representativeText: String?,
  val hearingStaff: Staff?,
  val internalLocation: InternalLocation?,
  val eventStatus: CodeDescription?,
  val hearingResults: List<HearingResult>,
  val eventId: Long?,
  @Schema(description = "Date time when the record was created the record in NOMIS", required = true)
  val createdDateTime: LocalDateTime,
  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,
  @Schema(description = "List of hearing notifications")
  val notifications: List<HearingNotification>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HearingNotification(
  @Schema(description = "Hearing notification date")
  val deliveryDate: LocalDate,
  @Schema(description = "Hearing notification time")
  val deliveryTime: LocalTime,
  @Schema(description = "Notification comment")
  val comment: String?,
  @Schema(description = "Staff notified")
  val notifiedStaff: Staff,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HearingResult(
  val pleaFindingType: CodeDescription?, // may not have a matching description if dodgy data
  val findingType: CodeDescription?,
  val charge: AdjudicationCharge,
  val offence: AdjudicationOffence,
  val resultAwards: List<HearingResultAward>,
  @Schema(description = "Date time when the record was created the record in NOMIS", required = true)
  val createdDateTime: LocalDateTime,
  @Schema(description = "Username of person who created the record in NOMIS", required = true)
  val createdByUsername: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HearingResultAward(
  @Schema(description = "Sequence of this sanction for this prisoner's booking", required = true)
  val sequence: Int,
  val sanctionType: CodeDescription?, // may not have a matching description if dodgy data
  val sanctionStatus: CodeDescription?,
  val comment: String?,
  val effectiveDate: LocalDate,
  val statusDate: LocalDate?,
  val sanctionDays: Int?,
  val sanctionMonths: Int?,
  val compensationAmount: BigDecimal?,
  val consecutiveAward: HearingResultAward?,
  val chargeSequence: Int,
  val adjudicationNumber: Long,
)

fun Offender.toPrisoner(createUsername: String, dateAddedToIncident: LocalDate, comment: String? = null) =
  Prisoner(
    offenderNo = nomsId,
    firstName = firstName,
    lastName = lastName,
    createdByUsername = createUsername,
    dateAddedToIncident = dateAddedToIncident,
    comment = comment,
  )
