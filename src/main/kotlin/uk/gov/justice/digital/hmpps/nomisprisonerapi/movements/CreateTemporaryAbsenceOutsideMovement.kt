package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Create temporary absence outside movement request")
data class CreateTemporaryAbsenceOutsideMovementRequest(
  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "From date")
  val fromDate: LocalDate,

  @Schema(description = "Release time")
  val releaseTime: LocalDateTime,

  @Schema(description = "To date")
  val toDate: LocalDate,

  @Schema(description = "Return time")
  val returnTime: LocalDateTime,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "To agency ID")
  val toAgencyId: String?,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Temporary absence type")
  val temporaryAbsenceType: String?,

  @Schema(description = "Temporary absence sub type")
  val temporaryAbsenceSubType: String?,
)

@Schema(description = "Create temporary absence outside movement response")
data class CreateTemporaryAbsenceOutsideMovementResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,

  @Schema(description = "Outside movement ID")
  val outsideMovementId: Long,
)
