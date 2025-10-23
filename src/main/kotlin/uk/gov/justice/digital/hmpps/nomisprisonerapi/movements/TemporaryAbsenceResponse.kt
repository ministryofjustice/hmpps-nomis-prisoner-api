package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Temporary absence response")
data class TemporaryAbsenceResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID. Empty for unscheduled movements.")
  val movementApplicationId: Long?,

  @Schema(description = "Scheduled temporary absence event ID. Empty for unscheduled movements.")
  val scheduledTemporaryAbsenceId: Long?,

  @Schema(description = "Movement sequence")
  val sequence: Int,

  @Schema(description = "Movement date")
  val movementDate: LocalDate,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason")
  val movementReason: String,

  @Schema(description = "Arresting Agency")
  val arrestAgency: String?,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "Escort text")
  val escortText: String?,

  @Schema(description = "From prison")
  val fromPrison: String?,

  @Schema(description = "To agency")
  val toAgency: String?,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,

  @Schema(description = "To address description")
  val toAddressDescription: String?,

  @Schema(description = "Full to address")
  val toFullAddress: String?,

  @Schema(description = "To address postcode")
  val toAddressPostcode: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)
