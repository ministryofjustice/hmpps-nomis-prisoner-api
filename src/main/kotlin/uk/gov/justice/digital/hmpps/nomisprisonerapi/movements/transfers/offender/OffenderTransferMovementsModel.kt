package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.offender

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.movement.TransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.TransferScheduleOut

@Schema(description = "Offender transfer movements by booking, including schedules")
data class OffenderTransferMovementsResponse(
  @Schema(description = "The prisoner number")
  val offenderNo: String,
  @Schema(description = "The root offender ID")
  val rootOffenderId: Long,
  @Schema(description = "List of bookings with their transfer movements")
  val bookings: List<BookingTransferMovements>,
)

@Schema(description = "Booking transfers")
data class BookingTransferMovements(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Whether this is an active booking")
  val activeBooking: Boolean,

  @Schema(description = "Whether this is the latest booking")
  val latestBooking: Boolean,

  @Schema(description = "List of transfer schedules")
  val transferSchedules: List<BookingTransferSchedule>,

  @Schema(description = "Unscheduled transfer movements - those without a schedule")
  val unscheduledTransferMovements: List<TransferMovementOut>,
)

@Schema(description = "Booking transfer schedule and movement")
data class BookingTransferSchedule(
  @Schema(description = "transfer schedule")
  val schedule: TransferScheduleOut,

  @Schema(description = "transfer movement")
  val movement: TransferMovementOut? = null,
)
