package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Allocation to an activity")
data class GetAllocationResponse(

  @Schema(description = "Prison ID", example = "BXI")
  val prisonId: String,

  @Schema(description = "Nomis Course Activity ID", example = "1234")
  val courseActivityId: Long,

  @Schema(description = "Nomis ID", example = "A1234BC")
  val nomisId: String,

  @Schema(description = "ID of the active booking", example = "12345")
  val bookingId: Long,

  @Schema(description = "Date allocated to the course", example = "2023-03-12")
  val startDate: LocalDate,

  @Schema(description = "Date deallocated from the course", example = "2023-05-26")
  val endDate: LocalDate? = null,

  @Schema(description = "Deallocation comment", example = "Removed due to schedule clash")
  val endComment: String? = null,

  @Schema(description = "Nomis reason code for ending (reference code domain PS_END_RSN)", example = "WDRAWN")
  val endReasonCode: String? = null,

  @Schema(description = "Whether the prisoner is currently suspended from the course", example = "false")
  val suspended: Boolean,

  @Schema(description = "Pay band", example = "1")
  val payBand: String? = null,

  @Schema(description = "Cell description (can be null if OUT or being transferred)", example = "RSI-A-1-001")
  val livingUnitDescription: String? = null,
)
