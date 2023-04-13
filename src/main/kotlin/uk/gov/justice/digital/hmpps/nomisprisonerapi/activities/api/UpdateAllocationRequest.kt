package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity update allocation request")
data class UpdateAllocationRequest(
  @Schema(
    description = "Booking id of the prisoner currently allocated to the activity",
    required = true,
    example = "1234567",
  )
  val bookingId: Long,

  @Schema(description = "Activity end date", required = true, example = "2022-08-12")
  val endDate: LocalDate,

  @Schema(description = "Activity end reason (from domain PS_END_RSN)", example = "REL")
  @field:Length(max = 12)
  val endReason: String? = null,

  @Schema(description = "Activity end comment")
  @field:Length(max = 240)
  val endComment: String? = null,
)
