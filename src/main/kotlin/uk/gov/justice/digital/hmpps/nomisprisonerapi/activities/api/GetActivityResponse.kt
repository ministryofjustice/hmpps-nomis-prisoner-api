package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Activity details")
data class GetActivityResponse(
  @Schema(description = "Activity id", example = "1")
  val courseActivityId: Long,

  @Schema(description = "Program service code", example = "INDUCTION")
  val programCode: String,

  @Schema(description = "Prison code", example = "RSI")
  val prisonId: String,

  @Schema(description = "Date course started", example = "2020-04-11")
  val startDate: LocalDate,

  @Schema(description = "Date course ended", example = "2023-11-15")
  val endDate: LocalDate?,

  @Schema(description = "Course internal location", example = "1234")
  val internalLocationId: Long?,

  @Schema(description = "Course internal location code", example = "KITCH")
  val internalLocationCode: String?,

  @Schema(description = "Course internal location description", example = "RSI-WORK_IND-KITCH")
  val internalLocationDescription: String?,

  @Schema(description = "Course capacity", example = "10")
  val capacity: Int,

  @Schema(description = "Course description", example = "Kitchen work")
  val description: String,

  @Schema(description = "The minimum incentive level allowed on the course", example = "BAS")
  val minimumIncentiveLevel: String,

  @Schema(description = "Whether the course runs on bank holidays", example = "false")
  val excludeBankHolidays: Boolean,

  @Schema(description = "Rules for creating schedules - days and times")
  val scheduleRules: List<ScheduleRulesResponse>,

  @Schema(description = "Pay rates available")
  val payRates: List<PayRatesResponse>,

  @Schema(description = "Prisoners allocated to the course")
  val allocations: List<AllocationsResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Activity Schedule Rules")
data class ScheduleRulesResponse(

  @Schema(description = "Course start time", example = "09:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "Course end time", example = "11:00")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "Runs on Mondays", example = "true")
  val monday: Boolean,

  @Schema(description = "Runs on Tuesdays", example = "true")
  val tuesday: Boolean,

  @Schema(description = "Runs on Wednesdays", example = "true")
  val wednesday: Boolean,

  @Schema(description = "Runs on Thursdays", example = "true")
  val thursday: Boolean,

  @Schema(description = "Runs on Fridays", example = "true")
  val friday: Boolean,

  @Schema(description = "Runs on Saturdays", example = "true")
  val saturday: Boolean,

  @Schema(description = "Runs on Sundays", example = "true")
  val sunday: Boolean,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Activity Pay Rates")
data class PayRatesResponse(

  @Schema(description = "Incentive level code", example = "BAS")
  val incentiveLevelCode: String,

  @Schema(description = "Pay band", example = "1")
  val payBand: String,

  @Schema(description = "rate", example = "3.2")
  val rate: BigDecimal,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Allocations")
data class AllocationsResponse(

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
