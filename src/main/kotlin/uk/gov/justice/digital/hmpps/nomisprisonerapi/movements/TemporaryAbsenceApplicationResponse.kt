package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Temporary absence application response")
data class TemporaryAbsenceApplicationResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Application date")
  val applicationDate: LocalDateTime,

  @Schema(description = "From date")
  val fromDate: LocalDate,

  @Schema(description = "Release time")
  val releaseTime: LocalDateTime,

  @Schema(description = "To date")
  val toDate: LocalDate,

  @Schema(description = "Return time")
  val returnTime: LocalDateTime,

  @Schema(description = "Application status")
  val applicationStatus: String,

  @Schema(description = "Escort code")
  val escortCode: String?,

  @Schema(description = "Transport type")
  val transportType: String?,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Prison ID")
  val prisonId: String?,

  @Schema(description = "To agency ID")
  val toAgencyId: String?,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Application type")
  val applicationType: String,

  @Schema(description = "Temporary absence type")
  val temporaryAbsenceType: String?,

  @Schema(description = "Temporary absence sub type")
  val temporaryAbsenceSubType: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)
