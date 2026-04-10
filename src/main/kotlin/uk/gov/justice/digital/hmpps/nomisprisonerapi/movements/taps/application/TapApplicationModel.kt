package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.application

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.UpsertTapAddress
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Tap application")
data class TapApplication(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Whether this is the latest booking")
  val latestBooking: Boolean,

  @Schema(description = "Whether this is an active booking")
  val activeBooking: Boolean,

  @Schema(description = "Tap application ID")
  val tapApplicationId: Long,

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

  @Schema(description = "To agency ID")
  val toAgencyId: String?,

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

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Application type")
  val applicationType: String,

  @Schema(description = "Temporary absence type")
  val tapType: String?,

  @Schema(description = "Temporary absence sub type")
  val tapSubType: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Upsert tap application request")
data class UpsertTapApplication(
  @Schema(description = "Existing PK, null if new")
  val tapApplicationId: Long? = null,

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
  val tapType: String?,

  @Schema(description = "Temporary absence sub type")
  val tapSubType: String?,

  @Schema(description = "To address. If this is null, do not update the address. Otherwise use the addressId in the request.")
  @Deprecated("Use toAddresses instead", ReplaceWith("toAddresses"))
  val toAddress: UpsertTapAddress? = null,

  @Schema(description = "To addresses linked to schedules. Makes sure they all exist in NOMIS so are available when upserting schedules.")
  val toAddresses: List<UpsertTapAddress> = emptyList(),
)

@Schema(description = "Upsert tap application response")
data class UpsertTapApplicationResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Movement application ID")
  val tapApplicationId: Long,
)
