package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

// TODO SDI-500 flesh out the request
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity update request")
data class UpdateActivityRequest(

  @Schema(description = "Prison where the activity is to occur", required = true)
  val prisonId: String,
)
