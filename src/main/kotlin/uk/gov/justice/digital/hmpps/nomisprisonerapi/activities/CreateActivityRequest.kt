package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity creation request")
data class CreateActivityRequest(

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

/*
 "prisonCode": "PVI",
  "attendanceRequired": false, -- ignore
  "summary": "Maths level 1", -- ignore
  "description": "A basic maths course suitable for introduction to the subject",
  "categoryId": 0,  program service code ***
  "tierId": 1,      --- ignore
  "eligibilityRuleIds": [  -- course_activity_profiles TBC
  ],
  "pay": [
    {
      "incentiveLevel": "Basic", -- desc not code
      "payBand": "A", -- 1 to 10 = nomis
      "rate": 150, *** half day rate
      "pieceRate": 150, -- ignore or might be a slot in nomis
      "pieceRateItems": 10 -- ignore
    }
  ],
  "riskLevel": "High", -- ignore
  "minimumIncentiveLevel": "Basic",
  "startDate": "2022-12-23",
  "endDate": "2022-12-23"

  NOTE slots being refactored out of the activity schedule.
schedule =
  {
  "id": 123456,
  "description": "Monday AM Houseblock 3",
  "suspensions": [
    {
      "suspendedFrom": "2022-12-23",
      "suspendedUntil": "2022-12-23"
    }
  ],
  "startTime": "9:00",
  "endTime": "11:30",
  "internalLocation": 98877667,
  "capacity": 10,
  "activity": {
    "id": 123456,
    "prisonCode": "PVI",
    "attendanceRequired": false,
    "summary": "Maths level 1",
    "description": "A basic maths course suitable for introduction to the subject",
    "category": {
      "id": 1,
      "code": "LEISURE_SOCIAL",
      "name": "Leisure and social",
      "description": "Such as association, library time and social clubs, like music or art"
    },
    "riskLevel": "High",
    "minimumIncentiveLevel": "Basic"
  }
}
 */
