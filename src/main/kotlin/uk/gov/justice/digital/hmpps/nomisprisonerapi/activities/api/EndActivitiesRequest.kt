package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "End activities request")
data class EndActivitiesRequest(
  @Schema(description = "Course activity ids", example = "[1, 2]")
  val courseActivityIds: Collection<Long>,
)
