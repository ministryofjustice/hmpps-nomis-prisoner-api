package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.movement

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Tap Movement Out")
data class TapMovementOut(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Tap application ID. Empty for unscheduled movements.")
  val tapApplicationId: Long?,

  @Schema(description = "Tap schedule out event ID. Empty for unscheduled movements.")
  val tapScheduleOutId: Long?,

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
  val fromPrison: String,

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
data class TapMovementIn(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Tap application ID. Empty for unscheduled movements.")
  val tapApplicationId: Long?,

  @Schema(description = "Tap scheduled out event ID. Empty for unscheduled movements.")
  val tapScheduleOutId: Long?,

  @Schema(description = "Tap schedule in event ID. Empty for unscheduled movements.")
  val tapScheduleInId: Long?,

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
  val toPrison: String,

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

@Schema(description = "Create tap movement out")
data class CreateTapMovementOut(
  @Schema(description = "Tap scheduled out event ID")
  val tapScheduleOutId: Long? = null,

  @Schema(description = "Movement date")
  val movementDate: LocalDate,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason code (ref domain MOVE_RSN)")
  val movementReason: String,

  @Schema(description = "Arresting agency code")
  val arrestAgency: String? = null,

  @Schema(description = "Escort code")
  val escort: String? = null,

  @Schema(description = "Escort text")
  val escortText: String? = null,

  @Schema(description = "From prison code")
  val fromPrison: String,

  @Schema(description = "To agency code")
  val toAgency: String? = null,

  @Schema(description = "Comment")
  val commentText: String? = null,

  @Schema(description = "To city code (ref domain CITY)")
  val toCity: String? = null,

  @Schema(description = "To address ID")
  val toAddressId: Long? = null,
)

@Schema(description = "Create tap movement out response")
data class CreateTapMovementOutResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val movementSequence: Int,
)

@Schema(description = "Create tap movement in request")
data class CreateTapMovementIn(
  @Schema(description = "Tap schedule in event ID")
  val tapScheduleInId: Long? = null,

  @Schema(description = "Movement date")
  val movementDate: LocalDate,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason")
  val movementReason: String,

  @Schema(description = "Arresting agency code")
  val arrestAgency: String? = null,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "Escort text")
  val escortText: String?,

  @Schema(description = "From agency")
  val fromAgency: String?,

  @Schema(description = "To prison")
  val toPrison: String,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "From address ID")
  val fromAddressId: Long?,
)

@Schema(description = "Create tap movement in response")
data class CreateTapMovementInResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val movementSequence: Int,
)
