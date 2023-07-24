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

  @Schema(description = "The evidence records as part of the incident")
  val evidence: List<EvidenceToCreate>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChargeToCreate(
  @Schema(description = "Offence code they are charged with", example = "51:1N")
  val offenceCode: String,
  @Schema(description = "Charges associated with this adjudication (business key)", example = "1234567/1")
  val offenceId: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class IncidentToCreate(
  @Schema(description = "Reporting staff member username", required = true, example = "JANE.BROOKES")
  val reportingStaffUsername: String,

  @Schema(description = "Date of the associated incident", required = true)
  val incidentDate: LocalDate,

  @Schema(description = "Date and time of the associated incident", required = true, example = "12:00:00")
  val incidentTime: LocalTime,

  @Schema(description = "Date when the associated incident was reported", required = true)
  val reportedDate: LocalDate,

  @Schema(description = "Date and time when the associated incident was reported", required = true, example = "12:00:00")
  val reportedTime: LocalTime,

  @Schema(description = "location id where incident took place", required = true)
  val internalLocationId: Long,

  @Schema(description = "Incident details", example = "The details of the incident are as follows")
  val details: String,

  @Schema(description = "Prison code where the incident took place", example = "MDI")
  val prisonId: String,

  @Schema(description = "Prisoners numbers that were victims in the incident")
  val prisonerVictimsOffenderNumbers: List<String>,

  @Schema(description = "Staff usernames that witnessed the incident")
  val staffWitnessesUsernames: List<String>,

  @Schema(description = "Staff usernames that were victims in the incident")
  val staffVictimsUsernames: List<String>,

  @Schema(description = "The repairs required due to the damage")
  val repairs: List<RepairToCreate>,

)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RepairToCreate(
  @Schema(description = "Type of repairs", example = "PLUM")
  val typeCode: String,
  @Schema(description = "Optional description of repairs", example = "Damage to the plumbing")
  val comment: String?,
  @Schema(description = "Optional cost of repairs", example = "62.12")
  val cost: BigDecimal?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EvidenceToCreate(
  @Schema(description = "Type of evidence", example = "PHOTO")
  val typeCode: String,
  @Schema(description = "Description of evidence", example = "Image of damages")
  val detail: String,
)
