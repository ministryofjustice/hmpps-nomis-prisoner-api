package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.offender

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Offender court movements by booking, including schedules")
data class OffenderCourtMovementsResponse(
  @Schema(description = "List of bookings with their court movements")
  val bookings: List<BookingCourtMovements>,
)

@Schema(description = "Booking court movements")
data class BookingCourtMovements(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Whether this is an active booking")
  val activeBooking: Boolean,

  @Schema(description = "Whether this is the latest booking")
  val latestBooking: Boolean,

  @Schema(description = "List of court scheduels")
  val courtSchedules: List<BookingCourtScheduleOut>,

  @Schema(description = "Unscheduled court movements OUT - those without a schedule")
  val unscheduledCourtMovementOuts: List<BookingCourtMovementOut>,

  @Schema(description = "Unscheduled court movements IN - those without a schedule")
  val unscheduledCourtMovementIns: List<BookingCourtMovementIn>,
)

data class BookingCourtScheduleOut(
  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Court movement out")
  val courtMovementOut: BookingCourtMovementOut? = null,

  @Schema(description = "Court movement in")
  val courtMovementIn: BookingCourtMovementIn? = null,

  @Schema(description = "Event date")
  val eventDate: LocalDate,

  @Schema(description = "Start time")
  val startTime: LocalDateTime,

  @Schema(description = "Event type")
  val eventType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String? = null,

  @Schema(description = "Prison code at time of scheduling")
  val prison: String,

  @Schema(description = "Court code")
  val court: String,

  @Schema(description = "Court case ID")
  val courtCaseId: Long? = null,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

data class BookingCourtMovementOut(

  @Schema(description = "Movement sequence")
  val sequence: Int,

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

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

data class BookingCourtMovementIn(

  @Schema(description = "Movement sequence")
  val sequence: Int,

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

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)
