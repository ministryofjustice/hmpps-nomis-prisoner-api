package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
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
  @Schema(description = "The start date of the booking", example = "2020-07-17")
  val startDate: LocalDate,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16")
  val endDate: LocalDate?,
  @Schema(description = "A list of physical attributes for this booking")
  val physicalAttributes: List<PhysicalAttributesResponse>,
)

@Schema(description = "Physical attributes recorded against a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PhysicalAttributesResponse(
  @Schema(description = "The height of the prisoner in centimetres", example = "180")
  val heightCentimetres: Int,
  @Schema(description = "The weight of the prisoner in kilograms", example = "80")
  val weightKilograms: Int,
  @Schema(description = "The time the physical attributes were created", example = "2020-07-17T12:34:56")
  val createDate: LocalDateTime,
  @Schema(description = "The time the physical attributes were last changed", example = "2021-07-16T12:34:56")
  val modifiedDate: LocalDateTime,
  @Schema(description = "The name of the module that last changed the physical attributes, indicates if this was NOMIS or the synchronisation service", example = "DPS_SYNCHRONISATION")
  val auditModuleName: String,
)
