package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation")
data class AmendVisitRequest(

  // TODO, same as create?

  @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
  @field:Size(
    max = 7,
    min = 7,
    message = "Noms Id must be 7 chars long"
  )
  @NotBlank
  val offenderNo: String,

  @Schema(description = "Visit start date and time", required = true)
  val startTime: LocalDateTime,

  @Schema(description = "Prison where the visit is to occur", required = true)
  @NotBlank
  val prisonId: String,

  @Schema(description = "Visitors", required = true)
  @NotEmpty
  val visitorPersonIds: List<Long>
)
