package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Identifying marks held against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerIdentifyingMarksResponse(
  @Schema(description = "The prisoner's unique identifier", example = "A1234AA")
  val offenderNo: String,
  @Schema(description = "A list of bookings and their identifying marks")
  val bookings: List<BookingIdentifyingMarksResponse>,
)

@Schema(description = "Identifying marks held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BookingIdentifyingMarksResponse(
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The start date of the booking", example = "2020-07-17T12:34:56")
  val startDateTime: LocalDateTime,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16T12:34:56")
  val endDateTime: LocalDateTime?,
  @Schema(description = "Whether this is the latest booking or not. Note that latest does not imply active.", example = "true")
  val latestBooking: Boolean,
  @Schema(description = "A list of identifying marks for this booking")
  val identifyingMarks: List<IdentifyingMarksResponse>,
)

@Schema(description = "Identifying marks")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IdentifyingMarksResponse(
  @Schema(description = "Multiple identifying marks records can be created for each booking", example = "1")
  val idMarksSeq: Long?,
  @Schema(description = "The body part code", example = "TORSO")
  val bodyPartCode: String,
  @Schema(description = "The mark type code", example = "TAT")
  val markTypeCode: String,
  @Schema(description = "The side code", example = "F")
  val sideCode: String?,
  @Schema(description = "The part orientation code", example = "LOW")
  val partOrientationCode: String?,
  @Schema(description = "Optional comments", example = "Scar above left eye")
  val commentText: String?,
  @Schema(description = "The IDs of images associated to the mark", example = "[12345, 56789]")
  val imageIds: List<Long>,
  @Schema(description = "The time the identifying marks were created", example = "2020-07-17T12:34:56")
  val createDateTime: LocalDateTime,
  @Schema(description = "The user who created the identifying marks", example = "AQ425D")
  val createdBy: String,
  @Schema(description = "The time the identifying marks were last changed", example = "2021-07-16T12:34:56")
  val modifiedDateTime: LocalDateTime?,
  @Schema(description = "The user who modified the identifying marks", example = "AQ425D")
  val modifiedBy: String?,
  @Schema(description = "The name of the module that last changed the identifying marks, indicates if this was NOMIS or the synchronisation service", example = "DPS_SYNCHRONISATION")
  val auditModuleName: String?,
)
