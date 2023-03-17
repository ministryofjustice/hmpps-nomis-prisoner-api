package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity creation request")
data class CreateAttendanceRequest(

  @Schema(description = "The status of the attendance", example = "SCH", required = true)
  val eventStatusCode: String,

  @Schema(description = "The outcome code for a completed attendance", example = "ATT")
  val eventOutcomeCode: String? = null,

  @Schema(description = "Comments relating to the attendance", example = "Disruptive")
  val comments: String? = null,

  @Schema(description = "Whether the absence is excused", example = "true", defaultValue = "false")
  val unexcusedAbsence: Boolean = false,

  @Schema(description = "Whether the absence is authorised", example = "true", defaultValue = "false")
  val authorisedAbsence: Boolean = false,

  @Schema(description = "Whether the attendance is to be paid", example = "true", defaultValue = "false")
  val paid: Boolean = false,

  @Schema(description = "Any bonus pay for the attendance", example = "1.50")
  val bonusPay: BigDecimal? = null,
)
