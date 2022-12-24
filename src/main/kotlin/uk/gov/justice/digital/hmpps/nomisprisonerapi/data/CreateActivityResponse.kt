package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Activity creation response")
data class CreateActivityResponse(
  @Schema(description = "The created course activity id", required = true)
  @NotNull
  val courseActivityId: Long,
)
