package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.physicalattributes

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Physical attributes held against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerPhysicalAttributesResponse(
  @Schema(description = "The prisoner's unique identifier", example = "A1234AA")
  val offenderNo: String,
  @Schema(description = "A list of bookings and their physical attributes")
  val bookings: List<BookingPhysicalAttributesResponse>,
)

@Schema(description = "Physical attributes held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BookingPhysicalAttributesResponse(
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The start date of the booking", example = "2020-07-17T12:34:56")
  val startDateTime: LocalDateTime,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16T12:34:56")
  val endDateTime: LocalDateTime?,
  @Schema(description = "A list of physical attributes for this booking")
  val physicalAttributes: List<PhysicalAttributesResponse>,
  @Schema(description = "Whether this is the latest booking or not. Note that latest does not imply active.", example = "true")
  val latestBooking: Boolean,
)

@Schema(description = "Physical attributes recorded against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PhysicalAttributesResponse(
  @Schema(description = "Multiple physical attribute records can be created for each booking", example = "1")
  val attributeSequence: Long?,
  @Schema(description = "The height of the prisoner in centimetres", example = "180")
  val heightCentimetres: Int?,
  @Schema(description = "The weight of the prisoner in kilograms", example = "80")
  val weightKilograms: Int?,
  @Schema(description = "The time the physical attributes were created", example = "2020-07-17T12:34:56")
  val createDateTime: LocalDateTime,
  @Schema(description = "The user who created the physical attributes", example = "AQ425D")
  val createdBy: String,
  @Schema(description = "The time the physical attributes were last changed", example = "2021-07-16T12:34:56")
  val modifiedDateTime: LocalDateTime?,
  @Schema(description = "The user who modified the physical attributes", example = "AQ425D")
  val modifiedBy: String?,
  @Schema(description = "The name of the module that last changed the physical attributes, indicates if this was NOMIS or the synchronisation service", example = "DPS_SYNCHRONISATION")
  val auditModuleName: String?,
)
