package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Create temporary absence return request")
data class CreateTemporaryAbsenceReturnRequest(
  @Schema(description = "Scheduled temporary absence return event ID")
  val scheduledTemporaryAbsenceReturnId: Long? = null,

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

@Schema(description = "Create temporary absence return response")
data class CreateTemporaryAbsenceReturnResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement sequence")
  val movementSequence: Int,
)
