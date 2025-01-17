package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
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
  @Deprecated("Currently being ignored - soon to be removed")
  val minimumIncentiveLevelCode: String?,

  @Schema(description = "Program Service code (from activity category)", required = true)
  val programCode: String,

  @Schema(description = "Half or Full day (H or F)", required = true, example = "H")
  val payPerSession: PayPerSession,

  @Schema(description = "Schedules", required = false)
  val schedules: List<CourseScheduleRequest> = listOf(),

  @Schema(description = "Schedule rules", required = false)
  val scheduleRules: List<ScheduleRuleRequest> = listOf(),

  @Schema(description = "Exclude bank holidays?", required = true)
  val excludeBankHolidays: Boolean,

  @Schema(description = "Outside work?", required = true)
  val outsideWork: Boolean,
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

  @Schema(description = "Pay rate start date, null means 'before every other rate'", example = "2022-08-12")
  val startDate: LocalDate? = null,

  @Schema(description = "Pay rate end date, if not passed will be derived from start dates", example = "2022-08-12")
  val endDate: LocalDate? = null,
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course schedule request")
data class CourseScheduleRequest(

  @Schema(description = "The id of the course schedule if known", example = "13245")
  val id: Long? = null,

  @Schema(description = "The date of the course schedule", example = "2023-04-03", required = true)
  val date: LocalDate,

  @Schema(description = "The time of the course schedule", example = "10:00", required = true)
  val startTime: LocalTime,

  @Schema(description = "The time the course schedule ends", example = "11:00", required = true)
  val endTime: LocalTime,

  @Schema(description = "Whether the course schedule has been cancelled", example = "true", required = false)
  val cancelled: Boolean = false,
)
