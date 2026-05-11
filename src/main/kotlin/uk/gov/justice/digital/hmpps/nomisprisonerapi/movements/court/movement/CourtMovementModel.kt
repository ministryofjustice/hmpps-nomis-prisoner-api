package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.movement

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Court Movement Out")
data class CourtMovementOut(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val sequence: Int,

  @Schema(description = "Schedule out ID")
  val eventId: Long?,

  @Schema(description = "Court schedule out event ID. Empty for unscheduled movements.")
  val courtScheduleOutId: Long?,

  @Schema(description = "Movement date")
  val movementDate: LocalDate,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason")
  val movementReason: String,

  @Schema(description = "From prison")
  val fromPrison: String,

  @Schema(description = "To court")
  val toCourt: String?,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "Audit user's active caseload ID (modified user else create user)")
  val userActiveCaseloadId: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Court Movement In")
data class CourtMovementIn(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val sequence: Int,

  @Schema(description = "Schedule out ID")
  val eventId: Long?,

  @Schema(description = "Court schedule out event ID. Empty for unscheduled movements.")
  val courtScheduleOutId: Long?,

  @Schema(description = "Movement date")
  val movementDate: LocalDate,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason")
  val movementReason: String,

  @Schema(description = "From court")
  val fromCourt: String?,

  @Schema(description = "To prison")
  val toPrison: String,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "Audit user's active caseload ID (modified user else create user)")
  val userActiveCaseloadId: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)
