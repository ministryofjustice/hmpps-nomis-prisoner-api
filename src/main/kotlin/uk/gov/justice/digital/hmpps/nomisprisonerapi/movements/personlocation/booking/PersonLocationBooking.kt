package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.booking

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.BookingStatus
import java.time.LocalDateTime

@Schema(description = "A single booking's person location details, without its movements")
data class PersonLocationBooking(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Booking Number")
  val bookingNumber: String,

  @Schema(description = "Whether this is an active booking")
  val activeBooking: Boolean,

  @Schema(description = "Whether this is the latest booking")
  val latestBooking: Boolean,

  @Schema(description = "The open/closed flag")
  val bookingStatus: BookingStatus,

  @Schema(description = "The booking begin date")
  val bookingBeginTime: LocalDateTime,

  @Schema(description = "The booking end date")
  val bookingEndTime: LocalDateTime?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)
