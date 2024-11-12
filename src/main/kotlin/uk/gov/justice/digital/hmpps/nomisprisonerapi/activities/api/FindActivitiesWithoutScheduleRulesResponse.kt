package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Active activities with allocations but no schedule rules")
data class FindActivitiesWithoutScheduleRulesResponse(
  @Schema(description = "Course Activity ID", example = "1234567")
  val courseActivityId: Long,
  @Schema(description = "Course description", example = "Kitchens AM")
  val courseActivityDescription: String,
)
