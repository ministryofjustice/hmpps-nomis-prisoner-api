package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Scheduled temporary absence response")
data class ScheduledTemporaryAbsenceResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,

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

  @Schema(description = "From address description")
  val toAddressDescription: String?,

  @Schema(description = "From full address")
  val toFullAddress: String?,

  @Schema(description = "From address postcode")
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
