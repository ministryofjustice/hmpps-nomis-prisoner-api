package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Temporary absence return response")
data class TemporaryAbsenceReturnResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID. Empty for unscheduled movements.")
  val movementApplicationId: Long?,

  @Schema(description = "Scheduled temporary absence event ID (outbound). Empty for unscheduled movements.")
  val scheduledTemporaryAbsenceId: Long?,

  @Schema(description = "Scheduled temporary absence return event ID (inbound). Empty for unscheduled movements.")
  val scheduledTemporaryAbsenceReturnId: Long?,

  @Schema(description = "Movement sequence")
  val sequence: Int,

  @Schema(description = "Movement date")
  val movementDate: LocalDate,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason")
  val movementReason: String,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "Escort text")
  val escortText: String?,

  @Schema(description = "From agency")
  val fromAgency: String?,

  @Schema(description = "To prison")
  val toPrison: String?,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "From address ID")
  val fromAddressId: Long?,

  @Schema(description = "From address owner class")
  val fromAddressOwnerClass: String?,

  @Schema(description = "From address description")
  val fromAddressDescription: String?,

  @Schema(description = "From full address")
  val fromFullAddress: String?,

  @Schema(description = "From address postcode")
  val fromAddressPostcode: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)
