package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Create scheduled temporary absence return request")
data class CreateScheduledTemporaryAbsenceReturnRequest(
  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,

  @Schema(description = "Scheduled temporary absence ID")
  val scheduledTemporaryAbsenceEventId: Long,

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
  val escort: String,

  @Schema(description = "From agency")
  val fromAgency: String?,

  @Schema(description = "To prison")
  val toPrison: String?,
)

@Schema(description = "Create scheduled temporary absence response")
data class CreateScheduledTemporaryAbsenceReturnResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,

  @Schema(description = "Event ID")
  val eventId: Long,
)
