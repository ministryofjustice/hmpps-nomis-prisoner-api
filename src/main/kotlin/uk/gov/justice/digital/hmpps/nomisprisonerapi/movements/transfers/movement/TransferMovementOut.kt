package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.movement

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDateTime

@Schema(description = "Transfer Movement Out")
data class TransferMovementOut(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val sequence: Int,

  @Schema(description = "Schedule out ID on the movement - this will be populated even if the transfer schedule no longer exists")
  val eventId: Long?,

  @Schema(description = "Transfer schedule out event ID. Empty for unscheduled or orphaned movements.")
  val transferScheduleOutId: Long?,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason")
  val movementReason: String,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "From prison")
  val fromPrison: String,

  @Schema(description = "To prison")
  val toPrison: String,

  @Schema(description = "Active flag")
  val active: Boolean,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,

  @Schema(description = "Audit user's active caseload ID (modified user else create user)")
  val userActiveCaseloadId: String?,
)
