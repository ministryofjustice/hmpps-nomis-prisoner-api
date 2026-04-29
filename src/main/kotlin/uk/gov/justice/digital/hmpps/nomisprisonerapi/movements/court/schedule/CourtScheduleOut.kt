package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Court schedule out response")
data class CourtScheduleOut(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Event date")
  val eventDate: LocalDate,

  @Schema(description = "Start time")
  val startTime: LocalDateTime,

  @Schema(description = "Event type")
  val eventType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Event status")
  val comment: String,

  @Schema(description = "Prison code at time of scheduling")
  val prison: String,

  @Schema(description = "Court case ID")
  val courtCaseId: Long? = null,
)
