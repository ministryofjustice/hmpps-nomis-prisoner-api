package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity creation request")
data class CreateActivityRequest(

  @Schema(description = "Code generated from the activity and schedule ids and mapped", required = true)
  @field:Length(min = 1, max = 12)
  val code: String,

  @Schema(description = "Activity start date", required = true, example = "2022-08-12")
  val startDate: LocalDate,

  @Schema(description = "Activity end date", example = "2022-08-12")
  val endDate: LocalDate?,

  @Schema(description = "Prison where the activity is to occur", required = true)
  val prisonId: String,

  @Schema(description = "Room where the activity is to occur (from activity schedule)")
  val internalLocationId: Long?,

  @Schema(description = "Capacity of activity (from activity schedule)", required = true)
  @field:Max(999)
  val capacity: Int,

  @Schema(description = "Pay rates", required = true)
  val payRates: List<PayRateRequest>,

  @Schema(description = "Description from concatenated activity and activity schedule", required = true)
  @field:Length(min = 1, max = 40)
  val description: String,

  @Schema(description = "Minimum Incentive Level")
  val minimumIncentiveLevelCode: String? = null,

  @Schema(description = "Program Service code (from activity category)", required = true)
  val programCode: String,

  @Schema(description = "Half or Full day (H or F)", required = true, example = "H")
  @field:Pattern(regexp = "[H|F]")
  val payPerSession: String,

  @Schema(description = "Schedules", required = false)
  val schedules: List<SchedulesRequest> = listOf(),

  @Schema(description = "Schedule rules", required = false)
  val scheduleRules: List<ScheduleRuleRequest> = listOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity creation request pay rates")
data class PayRateRequest(
  @Schema(description = "The incentive level", example = "BAS", required = true)
  val incentiveLevel: String,

  @Schema(description = "The pay band (1 TO 10)", example = "4", required = true)
  val payBand: String,

  @Schema(description = "The half day rate", example = "0.50", required = true)
  val rate: BigDecimal,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity creation request schedules")
data class SchedulesRequest(
  @Schema(description = "Schedule date", required = true, example = "2022-08-12")
  val date: LocalDate,

  @Schema(description = "Schedule start time in 24 hour clock", required = true, example = "08:00")
  val startTime: LocalTime,

  @Schema(description = "Schedule end time in 24 hour clock", required = true, example = "11:00")
  val endTime: LocalTime,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity creation request schedule rules")
data class ScheduleRuleRequest(
  @Schema(description = "Schedule start time in 24 hour clock", required = true, example = "08:00")
  val startTime: LocalTime,

  @Schema(description = "Schedule end time in 24 hour clock", required = true, example = "11:00")
  val endTime: LocalTime,

  @Schema(description = "Scheduled on Monday", required = true, example = "true")
  val monday: Boolean = false,

  @Schema(description = "Scheduled on Tuesday", required = true, example = "true")
  val tuesday: Boolean = false,

  @Schema(description = "Scheduled on Wednesday", required = true, example = "true")
  val wednesday: Boolean = false,

  @Schema(description = "Scheduled on Thursday", required = true, example = "true")
  val thursday: Boolean = false,

  @Schema(description = "Scheduled on Friday", required = true, example = "true")
  val friday: Boolean = false,

  @Schema(description = "Scheduled on Saturday", required = true, example = "false")
  val saturday: Boolean = false,

  @Schema(description = "Scheduled on Sunday", required = true, example = "false")
  val sunday: Boolean = false,
)
