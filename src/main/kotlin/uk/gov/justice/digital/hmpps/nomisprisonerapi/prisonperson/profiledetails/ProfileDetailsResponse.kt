package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.profiledetails

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Profile details held against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerProfileDetailsResponse(
  @Schema(description = "The prisoner's unique identifier", example = "A1234AA")
  val offenderNo: String,
  @Schema(description = "A list of bookings and their profile details")
  val bookings: List<BookingProfileDetailsResponse>,
)

@Schema(description = "Profile details held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BookingProfileDetailsResponse(
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The start date of the booking", example = "2020-07-17T12:34:56")
  val startDateTime: LocalDateTime,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16T12:34:56")
  val endDateTime: LocalDateTime?,
  @Schema(description = "A list of profile details for this booking")
  val profileDetails: List<ProfileDetailsResponse>,
  @Schema(description = "Whether this is the latest booking or not. Note that latest does not imply active.", example = "true")
  val latestBooking: Boolean,
)

@Schema(description = "Profile details recorded against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProfileDetailsResponse(
  @Schema(description = "The type of profile info", example = "BUILD")
  val type: String,
  @Schema(description = "The value of the profile info", example = "SLIM")
  val code: String?,
  @Schema(description = "The time the profile info was created", example = "2020-07-17T12:34:56")
  val createDateTime: LocalDateTime,
  @Schema(description = "The user who created the profile info", example = "AQ425D")
  val createdBy: String,
  @Schema(description = "The time the profile info was last changed", example = "2021-07-16T12:34:56")
  val modifiedDateTime: LocalDateTime?,
  @Schema(description = "The user who modified the profile info", example = "AQ425D")
  val modifiedBy: String?,
  @Schema(description = "The name of the module that last changed the profile info, indicates if this was NOMIS or the synchronisation service", example = "DPS_SYNCHRONISATION")
  val auditModuleName: String?,
)
