package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity creation request")
data class CreateOffenderProgramProfileRequest(

  @Schema(description = "Course activity id", required = true)
  @NotNull
  val courseActivityId: Long,

  @Schema(description = "Booking id of the prisoner to be allocated to the activity", required = true)
  @NotNull
  val bookingId: Long,

  @Schema(description = "Activity start date", required = true, example = "2022-08-12")
  @NotNull
  val startDate: LocalDate,

  @Schema(description = "Activity end date", example = "2022-08-12")
  val endDate: LocalDate? = null,
)
