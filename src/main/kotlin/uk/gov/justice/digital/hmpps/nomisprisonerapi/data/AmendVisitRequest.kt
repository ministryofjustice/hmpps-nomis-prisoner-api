package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation")
data class AmendVisitRequest(

  // TODO, same as create?

  @Schema(description = "Visit start date and time", required = true)
  val startTime: LocalDateTime,

  @Schema(description = "Prison where the visit is to occur", required = true)
  @NotBlank
  val prisonId: String,

  @Schema(description = "Visitors", required = true)
  @NotEmpty
  val visitorPersonIds: List<Long>
)
