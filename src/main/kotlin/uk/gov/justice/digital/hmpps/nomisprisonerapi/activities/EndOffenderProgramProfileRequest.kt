package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity deallocation request")
data class EndOffenderProgramProfileRequest(
  @Schema(description = "Activity end date", required = true, example = "2022-08-12")
  val endDate: LocalDate,

  @Schema(description = "Activity end reason (from domain PS_END_RSN)", example = "REL")
  @field:Length(max = 12)
  val endReason: String? = null,

  @Schema(description = "Activity end comment")
  @field:Length(max = 240)
  val endComment: String? = null,
)
