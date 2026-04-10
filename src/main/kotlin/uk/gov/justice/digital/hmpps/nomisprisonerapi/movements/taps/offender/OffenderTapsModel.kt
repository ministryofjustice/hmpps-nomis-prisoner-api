package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Offender taps by booking, including applications, schedules and movements")
data class OffenderTapsResponse(
  @Schema(description = "List of bookings with their taps")
  val bookings: List<BookingTaps>,
)

@Schema(description = "Booking taps")
data class BookingTaps(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Tap applications")
  val tapApplications: List<BookingTapApplication>,

  @Schema(description = "Unscheduled tap movements OUT - those without an application or a schedule")
  val unscheduledTapMovementOuts: List<BookingTapMovementOut>,

  @Schema(description = "Unscheduled tap movements IN - those without an application or a schedule")
  val unscheduledTapMovementIns: List<BookingTapMovementIn>,

  @Schema(description = "Whether this is an active booking")
  val activeBooking: Boolean,

  @Schema(description = "Whether this is the latest booking")
  val latestBooking: Boolean,
)

@Schema(description = "Tap application response")
data class BookingTapApplication(
  @Schema(description = "Tap application ID")
  val tapApplicationId: Long,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Application date")
  val applicationDate: LocalDate,

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
  val prisonId: String,

  @Schema(description = "To agency ID")
  val toAgencyId: String?,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,

  @Schema(description = "To address description")
  val toAddressDescription: String?,

  @Schema(description = "To full address")
  val toFullAddress: String?,

  @Schema(description = "To address postcode")
  val toAddressPostcode: String?,

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Application type")
  val applicationType: String,

  @Schema(description = "Tap type")
  val tapType: String?,

  @Schema(description = "Tap sub type")
  val tapSubType: String?,

  @Schema(description = "All taps")
  val taps: List<BookingTap> = mutableListOf(),

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "A single instance of a tap including schedules and movements in and out")
data class BookingTap(

  @Schema(description = "Tap schedule out")
  val tapScheduleOut: BookingTapScheduleOut? = null,

  @Schema(description = "Tap schedule in")
  val tapScheduleIn: BookingTapScheduleIn? = null,

  @Schema(description = "Tap movement out")
  val tapMovementOut: BookingTapMovementOut? = null,

  @Schema(description = "Tap movement in")
  val tapMovementIn: BookingTapMovementIn? = null,
)

@Schema(description = "Tap schedule out")
data class BookingTapScheduleOut(
  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Event date")
  val eventDate: LocalDate,

  @Schema(description = "Start time")
  val startTime: LocalDateTime,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "From prison")
  val fromPrison: String?,

  @Schema(description = "To agency")
  val toAgency: String?,

  @Schema(description = "Transport type")
  val transportType: String?,

  @Schema(description = "Return date")
  val returnDate: LocalDate,

  @Schema(description = "Return time")
  val returnTime: LocalDateTime,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,

  @Schema(description = "To address description")
  val toAddressDescription: String?,

  @Schema(description = "To full address")
  val toFullAddress: String?,

  @Schema(description = "TO address postcode")
  val toAddressPostcode: String?,

  @Schema(description = "Application date")
  val applicationDate: LocalDateTime,

  @Schema(description = "Application time")
  val applicationTime: LocalDateTime?,

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Tap schedule in")
data class BookingTapScheduleIn(
  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Event date")
  val eventDate: LocalDate,

  @Schema(description = "Start time")
  val startTime: LocalDateTime,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "From agency")
  val fromAgency: String?,

  @Schema(description = "To prison")
  val toPrison: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Tap movement out")
data class BookingTapMovementOut(
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

@Schema(description = "Tap movement in")
data class BookingTapMovementIn(
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

@Schema(description = "Tap counts")
data class TapSummary(
  @Schema(description = "The application counts")
  val applications: ApplicationSummary,
  @Schema(description = "The schedule out counts")
  val scheduledOuts: ScheduledOutSummary,
  @Schema(description = "The actual movement counts")
  val movements: MovementSummary,
)

@Schema(description = "Offender tap application counts")
data class ApplicationSummary(
  @Schema(description = "The number of applications")
  val count: Long,
)

@Schema(description = "Offender tap schedule OUT counts")
data class ScheduledOutSummary(
  @Schema(description = "The number of schedules OUT")
  val count: Long,
)

@Schema(description = "Offender tap movement counts")
data class MovementSummary(
  @Schema(description = "The number of actual movements")
  val count: Long,
  @Schema(description = "The number of scheduled movements by direction")
  val scheduled: MovementsByDirection,
  @Schema(description = "The number of unscheduled movements by direction")
  val unscheduled: MovementsByDirection,
)

@Schema(description = "Offender tap movement counts")
data class MovementsByDirection(
  @Schema(description = "The number of actual OUT movements")
  val outCount: Long,
  @Schema(description = "The number of actual IN movements")
  val inCount: Long,
)

@Schema(description = "Offender taps ids by booking, including applications and scheduled absences")
data class OffenderTapsIdsResponse(
  @Schema(description = "List of TAP application IDs")
  val applicationIds: List<Long>,

  @Schema(description = "List of TAP scheduled OUT IDs")
  val scheduleOutIds: List<Long>,

  @Schema(description = "List of TAP scheduled IN IDs")
  val scheduleInIds: List<Long>,

  @Schema(description = "List of TAP scheduled movement OUT IDs")
  val scheduledMovementOutIds: List<OffenderTapMovementId>,

  @Schema(description = "List of TAP scheduled movement IN IDs")
  val scheduledMovementInIds: List<OffenderTapMovementId>,

  @Schema(description = "List of TAP unscheduled movement OUT IDs")
  val unscheduledMovementOutIds: List<OffenderTapMovementId>,

  @Schema(description = "List of TAP unscheduled movement IN IDs")
  val unscheduledMovementInIds: List<OffenderTapMovementId>,
)

@Schema(description = "The ID of a single movement")
data class OffenderTapMovementId(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val sequence: Int,
)
