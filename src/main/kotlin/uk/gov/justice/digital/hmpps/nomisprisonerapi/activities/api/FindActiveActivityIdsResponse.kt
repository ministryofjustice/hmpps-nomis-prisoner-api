package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Find active activity ids response")
data class FindActiveActivityIdsResponse(
  @Schema(description = "Activity id", example = "1")
  val courseActivityId: Long,
  @Schema(description = "Does the activity have schedule rules?", example = "true")
  val hasScheduleRules: Boolean,
)
