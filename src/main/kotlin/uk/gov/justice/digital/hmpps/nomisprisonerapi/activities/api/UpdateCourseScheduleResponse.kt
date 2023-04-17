package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course schedule update update response")
data class UpdateCourseScheduleResponse(

  @Schema(description = "The id of the course schedule", example = "123456", required = true)
  val courseScheduleId: Long,
)
