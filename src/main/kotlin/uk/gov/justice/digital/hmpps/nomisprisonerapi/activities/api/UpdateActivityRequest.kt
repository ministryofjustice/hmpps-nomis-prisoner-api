package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity update request")
data class UpdateActivityRequest(

  @Schema(description = "Activity start date", required = true, example = "2022-08-12")
  val startDate: LocalDate,

  @Schema(description = "Activity end date", example = "2022-08-12")
  val endDate: LocalDate?,

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
  val minimumIncentiveLevelCode: String,

  @Schema(description = "Half or Full day (H or F)", required = true, example = "H")
  val payPerSession: PayPerSession,

  @Schema(description = "Schedule rules", required = false)
  val scheduleRules: List<ScheduleRuleRequest> = listOf(),

  @Schema(description = "Exclude bank holidays?", required = true)
  val excludeBankHolidays: Boolean,

  @Schema(description = "Outside work?", required = true)
  val outsideWork: Boolean,

  @Schema(description = "Program Service code (from activity category)", required = true)
  val programCode: String,

  @Schema(description = "Schedules", required = false)
  val schedules: List<CourseScheduleRequest> = listOf(),
)
