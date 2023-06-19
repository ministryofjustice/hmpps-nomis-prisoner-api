package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Adjudication Information")
data class AdjudicationResponse(

  @Schema(
    description = "The adjudication sequence, part of the composite key with adjudicationIncidentId",
    required = true,
  )
  val adjudicationSequence: Int,

  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,

  @Schema(description = "The id of the booking associated with the adjudication", required = true)
  val bookingId: Long,

  @Schema(description = "The adjudication number (business key)")
  val adjudicationNumber: Long? = null,

  @Schema(description = "Date Prisoner was added to the adjudication ????", required = true)
  val partyAddedDate: LocalDate,

  @Schema(description = "Adjudication comments")
  val comment: String? = null,

  @Schema(description = "Associated incident details")
  val incident: AdjudicationIncident,

  @Schema(description = "Charges associated with this adjudication")
  val charges: List<AdjudicationCharge>,
)

data class AdjudicationCharge(
  val offence: AdjudicationOffence,
  val evidence: String?,
  val reportDetail: String?,
  val offenceId: String?,
  val chargeSequence: Int,
)

data class AdjudicationOffence(
  val code: String,
  val description: String,
  val type: CodeDescription? = null,
  val category: CodeDescription? = null,
)

data class AdjudicationIncident(
  @Schema(
    description = "The adjudication incident Id, part of the composite key with adjudicationSequence",
    required = true,
  )
  val adjudicationIncidentId: Long,

  @Schema(description = "Reporting staff member", required = true)
  val reportingStaff: ReportingStaff,

  @Schema(description = "Date of the associated incident", required = true)
  val incidentDate: LocalDate,

  @Schema(description = "Date and time of the associated incident", required = true)
  val incidentTime: LocalTime,

  @Schema(description = "Date when the associated incident was reported", required = true)
  val reportedDate: LocalDate,

  @Schema(description = "Date and time when the associated incident was reported", required = true)
  val reportedTime: LocalTime,

  @Schema(description = "location where incident took place", required = true)
  val internalLocation: InternalLocation,

  @Schema(description = "Incident type ", required = true)
  val incidentType: CodeDescription,

  @Schema(description = "Incident details")
  val details: String? = null,

  @Schema(description = "Prison where the incident took place")
  val prison: CodeDescription,
)

data class ReportingStaff(
  @Schema(description = "NOMIS staff id")
  val staffId: Long,
  @Schema(description = "First name of staff member")
  val firstName: String,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)

fun Staff.toReportingStaff() = ReportingStaff(staffId = this.id, firstName = this.firstName, lastName = this.lastName)

data class InternalLocation(
  val locationId: Long,
  val code: String,
  val description: String,
)

fun AgencyInternalLocation.toInternalLocation() =
  InternalLocation(locationId = this.locationId, code = this.locationCode, description = this.description)

fun AdjudicationIncidentCharge.toCharge(): AdjudicationCharge = AdjudicationCharge(
  offence = AdjudicationOffence(
    code = this.offence.code,
    description = this.offence.description,
    type = this.offence.type?.toCodeDescription(),
  ),
  evidence = this.guiltyEvidence,
  reportDetail = this.reportDetails,
  offenceId = this.offenceId,
  chargeSequence = this.id.chargeSequence,
)
