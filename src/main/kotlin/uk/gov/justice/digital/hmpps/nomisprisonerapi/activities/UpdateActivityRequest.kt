package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity update request")
data class UpdateActivityRequest(

  @Schema(description = "Code generated from the activity and schedule ids and mapped", required = true)
  val code: String,

  @Schema(description = "Activity start date", required = true, example = "2022-08-12")
  val startDate: LocalDate,

  @Schema(description = "Activity end date", example = "2022-08-12")
  val endDate: LocalDate?,

  @Schema(description = "Prison where the activity is to occur", required = true)
  val prisonId: String,

  @Schema(description = "Room where the activity is to occur (from activity schedule)", required = true)
  val internalLocationId: Long,

  @Schema(description = "Capacity of activity (from activity schedule)", required = true)
  val capacity: Int,

  @Schema(description = "Pay rates", required = true)
  val payRates: List<PayRateRequest>,

  @Schema(description = "Description from concatenated activity and activity schedule", required = true)
  val description: String,

  @Schema(description = "Minimum Incentive Level")
  val minimumIncentiveLevelCode: String? = null,

  @Schema(description = "Program Service code (from activity category)", required = true)
  val programCode: String,
)
