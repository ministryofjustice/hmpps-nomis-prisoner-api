package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Upsert temporary absence application request")
data class UpsertTemporaryAbsenceApplicationRequest(
  @Schema(description = "Existing PK, null if new")
  val movementApplicationId: Long? = null,

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

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Application type")
  val applicationType: String,

  @Schema(description = "Temporary absence type")
  val temporaryAbsenceType: String?,

  @Schema(description = "Temporary absence sub type")
  val temporaryAbsenceSubType: String?,

  @Schema(description = "To address. If this is null, do not update the address. Otherwise use the addressId in the request.")
  val toAddress: UpsertTemporaryAbsenceAddress?,
)

@Schema(description = "Upsert temporary absence application response")
data class UpsertTemporaryAbsenceApplicationResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,
)
