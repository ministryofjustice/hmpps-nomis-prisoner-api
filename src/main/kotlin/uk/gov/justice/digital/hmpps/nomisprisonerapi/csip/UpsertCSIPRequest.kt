package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSIP Report create/update request")
data class UpsertCSIPRequest(

  @Schema(description = "The csip id", example = "1234")
  val id: Long? = 0,
  @Schema(description = "The offender No", example = "A11235BC", required = true)
  val offenderNo: String,

  @Schema(description = "Log number")
  val logNumber: String? = null,

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

  @Schema(description = "Username of person that created/updated the record (might also be a system) ")
  val createUsername: String,
)
