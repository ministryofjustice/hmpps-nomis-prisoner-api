package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Transfer schedule out response")
data class TransferScheduleOut(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Start time")
  val startTime: LocalDateTime?,

  @Schema(description = "Event sub type, aka movement reason")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Hidden Comment")
  val hiddenComment: String?,

  @Schema(description = "From prison")
  val fromPrison: String,

  @Schema(description = "To prison")
  val toPrison: String?,

  @Schema(description = "Cancellation reason")
  val cancellationReasonCode: String?,

  @Schema(description = "Escort code")
  val escortCode: String?,

  @Schema(description = "The waitlist")
  val waitlist: TransferScheduleWaitlist?,

  @Schema(description = "Audit data associated with the schedule")
  val audit: NomisAudit,

  @Schema(description = "Audit user's active caseload ID (modified user else create user)")
  val userActiveCaseloadId: String?,
)

@Schema(description = "Transfer schedule waitlist")
data class TransferScheduleWaitlist(
  @Schema(description = "Requested date")
  val requestDate: LocalDate,

  @Schema(description = "Waitlist status")
  val status: String,

  @Schema(description = "The date the status was changed")
  val statusDate: LocalDate,

  @Schema(description = "The transfer priority")
  val priority: String,

  @Schema(description = "Is the transfer approved?")
  val approved: Boolean,

  @Schema(description = "Approved by user")
  val approvedUserName: String?,

  @Schema(description = "Cancellation reason")
  val cancellationReasonCode: String?,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Audit data associated with the schedule")
  val audit: NomisAudit,

  @Schema(description = "Audit user's active caseload ID (modified user else create user)")
  val userActiveCaseloadId: String?,
)

@Schema(description = "Upsert transfer schedule out request")
data class UpsertTransferScheduleOut(
  @Schema(description = "Event ID")
  val eventId: Long? = null,

  @Schema(description = "Start time")
  val startTime: LocalDateTime? = null,

  @Schema(description = "Event sub type, aka movement reason")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String? = null,

  @Schema(description = "From prison")
  val fromPrison: String,

  @Schema(description = "To prison")
  val toPrison: String? = null,

  @Schema(description = "Escort code")
  val escortCode: String? = null,

  @Schema(description = "The waitlist")
  val waitlist: UpsertTransferScheduleWaitlist? = null,
)

@Schema(description = "Upsert transfer schedule waitlist request")
data class UpsertTransferScheduleWaitlist(
  @Schema(description = "Requested date")
  val requestDate: LocalDate,

  @Schema(description = "Waitlist status")
  val status: String,

  @Schema(description = "The date the status was changed")
  val statusDate: LocalDate,

  @Schema(description = "The transfer priority")
  val priority: String,

  @Schema(description = "Is the transfer approved?")
  val approved: Boolean,

  @Schema(description = "Approved by user")
  val approvedUserName: String? = null,

  @Schema(description = "Comment")
  val comment: String? = null,
)

@Schema(description = "Upsert transfer schedule response")
data class UpsertTransferScheduleOutResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Event ID")
  val eventId: Long,
)
