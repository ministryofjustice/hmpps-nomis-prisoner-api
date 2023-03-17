package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity allocation request")
data class CreateOffenderProgramProfileRequest(
  @Schema(
    description = "Booking id of the prisoner to be allocated to the activity",
    required = true,
    example = "1234567",
  )
  val bookingId: Long,

  @Schema(description = "Activity start date", required = true, example = "2022-08-12")
  val startDate: LocalDate,

  @Schema(description = "Activity end date", example = "2022-08-12")
  val endDate: LocalDate? = null,

  @Schema(description = "The prisoner's pay band", example = "2")
  @field:NotBlank
  @field:Length(min = 1, max = 12)
  val payBandCode: String,
)
