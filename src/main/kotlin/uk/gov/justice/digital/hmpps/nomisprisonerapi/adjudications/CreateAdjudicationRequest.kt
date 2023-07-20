package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Core Adjudication to be created")
data class CreateAdjudicationRequest(

  @Schema(description = "The adjudication number (business key)")
  val adjudicationNumber: Long,

  @Schema(description = "Associated incident details")
  val incident: IncidentToCreate,

  @Schema(description = "Charges associated with this adjudication")
  val charges: List<ChargeToCreate>,

)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChargeToCreate(
  val offenceCode: String,
  @Schema(description = "Charges associated with this adjudication (business key)")
  val offenceId: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class IncidentToCreate(
  @Schema(description = "Reporting staff member username", required = true)
  val reportingStaffUsername: String,

  @Schema(description = "Date of the associated incident", required = true)
  val incidentDate: LocalDate,

  @Schema(description = "Date and time of the associated incident", required = true)
  val incidentTime: LocalTime,

  @Schema(description = "Date when the associated incident was reported", required = true)
  val reportedDate: LocalDate,

  @Schema(description = "Date and time when the associated incident was reported", required = true)
  val reportedTime: LocalTime,

  @Schema(description = "location id where incident took place", required = true)
  val internalLocationId: Long,

  @Schema(description = "Incident details")
  val details: String,

  @Schema(description = "Prison code where the incident took place")
  val prisonId: String,

  @Schema(description = "Prisoners numbers that were victims in the incident")
  val prisonerVictimsOffenderNumbers: List<String>,

  @Schema(description = "Staff usernames that witnessed the incident")
  val staffWitnessesUsernames: List<String>,

  @Schema(description = "Staff usernames that were victims in the incident")
  val staffVictimsUsernames: List<String>,

  @Schema(description = "The repairs required due to the damage")
  val repairs: List<RepairToCreate>,

  @Schema(description = "The evidence records as part of the incident")
  val evidence: List<EvidenceToCreate>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RepairToCreate(
  val typeCode: String,
  val comment: String?,
  val cost: BigDecimal?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EvidenceToCreate(
  val typeCode: String,
  val detail: String,
)
