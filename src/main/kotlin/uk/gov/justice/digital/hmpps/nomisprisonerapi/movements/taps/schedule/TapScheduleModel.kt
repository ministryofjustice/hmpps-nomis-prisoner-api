package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.UpsertTapAddress
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Tap schedule out response")
data class TapScheduleOut(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID")
  val tapApplicationId: Long,

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

  @Schema(description = "Inbound event status")
  val inboundEventStatus: String?,

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

  @Schema(description = "To address postcode")
  val toAddressPostcode: String?,

  @Schema(description = "Application date")
  val applicationDate: LocalDateTime,

  @Schema(description = "Application time")
  val applicationTime: LocalDateTime?,

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Temporary absence type")
  val tapAbsenceType: String?,

  @Schema(description = "Temporary absence sub type")
  val tapSubType: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Tap schedule in response")
data class TapScheduleIn(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID")
  val tapApplicationId: Long,

  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Event ID")
  val parentEventId: Long,

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

@Schema(description = "Upsert tap schedule out request")
data class UpsertTapScheduleOut(
  @Schema(description = "Event ID")
  val eventId: Long?,

  @Schema(description = "Tap application ID")
  val tapApplicationId: Long,

  @Schema(description = "Event date")
  val eventDate: LocalDate,

  @Schema(description = "Start time")
  val startTime: LocalDateTime,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Event status of the return schedule")
  val returnEventStatus: String?,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "From prison")
  val fromPrison: String,

  @Schema(description = "To agency")
  val toAgency: String?,

  @Schema(description = "Transport type")
  val transportType: String?,

  @Schema(description = "Return date")
  val returnDate: LocalDate,

  @Schema(description = "Return time")
  val returnTime: LocalDateTime,

  @Schema(description = "To address")
  val toAddress: UpsertTapAddress,

  @Schema(description = "Application date")
  val applicationDate: LocalDateTime,

  @Schema(description = "Application time")
  val applicationTime: LocalDateTime?,
)

@Schema(description = "Upsert scheduled tap response")
data class UpsertTapScheduleOutResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Tap application ID")
  val tapApplicationId: Long,

  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Address ID")
  val addressId: Long,

  @Schema(description = "Address owner class")
  val addressOwnerClass: String,
)
