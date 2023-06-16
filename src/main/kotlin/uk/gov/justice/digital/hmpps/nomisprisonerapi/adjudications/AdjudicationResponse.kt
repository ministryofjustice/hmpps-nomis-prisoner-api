package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Adjudication Information")
data class AdjudicationResponse(
  @Schema(description = "The adjudication incident Id, part of the composite key with adjudicationSequence", required = true)
  val adjudicationIncidentId: Long,

  @Schema(description = "The adjudication sequence, part of the composite key with adjudicationIncidentId", required = true)
  val adjudicationSequence: Int,

  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,

  @Schema(description = "The adjudication number (business key)")
  val adjudicationNumber: Long? = null,

  @Schema(description = "Date Prisoner was added to the adjudication ????", required = true)
  val partyAddedDate: LocalDate,

  @Schema(description = "Adjudication comments")
  val comment: String? = null,

  // adjudication incident

  @Schema(description = "Reporting staff member Id", required = true)
  val reportingStaffId: Long,

  @Schema(description = "Date of the associated incident", required = true)
  val incidentDate: LocalDate,

  @Schema(description = "Date and time of the associated incident", required = true)
  val incidentTime: LocalTime,

  @Schema(description = "Date when the associated incident was reported", required = true)
  val reportedDate: LocalDate,

  @Schema(description = "Date and time when the associated incident was reported", required = true)
  val reportedTime: LocalTime,

  @Schema(description = "NOMIS room id", required = true)
  val internalLocationId: Long,

  @Schema(description = "Incident type ", required = true)
  val incidentType: CodeDescription,

  val incidentStatus: String = "ACTIVE",

  @Schema(description = "Incident details")
  val incidentDetails: String? = null,

  @Schema(description = "Prison where the incident took place ??????")
  val prisonId: String,

)
