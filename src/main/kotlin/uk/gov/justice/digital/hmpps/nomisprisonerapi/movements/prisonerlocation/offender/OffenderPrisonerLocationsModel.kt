package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.prisonerlocation.offender

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDateTime

@Schema(description = "Offender prisoner locations, with bookings and movements")
data class OffenderPrisonerLocationsResponse(
  @Schema(description = "List of bookings with their external movements")
  val bookings: List<BookingPrisonerLocations>,
)

@Schema(description = "Booking court movements")
data class BookingPrisonerLocations(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Booking Number")
  val bookingNumber: String,

  @Schema(description = "Whether this is an active booking")
  val activeBooking: Boolean,

  @Schema(description = "Whether this is the latest booking")
  val latestBooking: Boolean,

  @Schema(description = "The open/closed flag ")
  val bookingStatus: BookingStatus,

  @Schema(description = "The booking begin date ")
  val bookingBeginTime: LocalDateTime,

  @Schema(description = "The booking end date ")
  val bookingEndTime: LocalDateTime,

  @Schema(description = "Movements related to the booking")
  val movements: List<PrisonerLocationMovement>,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

data class PrisonerLocationMovement(

  @Schema(description = "Movement sequence")
  val sequence: Int,

  @Schema(description = "Movement type")
  val movementType: MovementType,

  @Schema(description = "The direction of the movement")
  val directionCode: MovementDirection,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason code")
  val movementReason: CodeDescription,

  @Schema(description = "From location")
  val from: MovementLocation,

  @Schema(description = "To location")
  val to: MovementLocation?,

  @Schema(description = "Schedule ID")
  val scheduleId: Long?,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

enum class BookingStatus { CLOSED, OPEN }
enum class MovementDirection { IN, OUT }
enum class MovementType { ADM, CRT, TAP, TRN, REL }
enum class MovementLocationType { PRISON, COURT, OTHER }

data class MovementLocation(
  val code: String?,
  val type: MovementLocationType,
  val description: String,
)
