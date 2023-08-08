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

  @Schema(description = "Half or Full day (H or F)", required = true, example = "H")
  val payPerSession: String,

  @Schema(description = "Rules for creating schedules - days and times")
  val scheduleRules: List<ScheduleRulesResponse>,

  @Schema(description = "Pay rates available")
  val payRates: List<PayRatesResponse>,
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
