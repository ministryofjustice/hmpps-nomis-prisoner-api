package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Create temporary absence request")
data class CreateTemporaryAbsenceRequest(
  @Schema(description = "Scheduled temporary absence event ID")
  val scheduledTemporaryAbsenceId: Long? = null,

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
  val fromPrison: String? = null,

  @Schema(description = "To agency code")
  val toAgency: String? = null,

  @Schema(description = "Comment")
  val commentText: String? = null,

  @Schema(description = "To city code (ref domain CITY)")
  val toCity: String? = null,

  @Schema(description = "To address ID")
  val toAddressId: Long? = null,
)

@Schema(description = "Create scheduled temporary absence response")
data class CreateTemporaryAbsenceResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val movementSequence: Int,
)
