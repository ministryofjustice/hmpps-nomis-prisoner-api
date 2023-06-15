package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import java.time.LocalDate
import java.time.LocalDateTime

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

  @Schema(description = "")
  val incidentRole: String,

  @Schema(description = "Id of the staff member ??????????")
  val staffId: Long? = null,

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
  val incidentDateTime: LocalDateTime,

  @Schema(description = "Date when the associated incident was reported", required = true)
  val reportedDate: LocalDate = LocalDate.now(),

  @Schema(description = "Date and time when the associated incident was reported", required = true)
  val reportedDateTime: LocalDateTime = LocalDateTime.now(),

  @Schema(description = "NOMIS room id", required = true)
  val internalLocation: Long,

  @Schema(description = "Incident type ", required = true)
  val incidentType: CodeDescription,

  val incidentStatus: String = "ACTIVE",

  @Schema(description = "Incident details")
  val incidentDetails: String? = null,

  @Schema(description = "Prison where the incident took place ??????")
  val prison: AgencyLocation,

  )