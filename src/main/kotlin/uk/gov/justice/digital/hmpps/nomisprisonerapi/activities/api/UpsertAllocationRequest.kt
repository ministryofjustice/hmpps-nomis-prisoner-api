package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import org.springframework.validation.annotation.Validated
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity create or update allocation request")
@Validated
data class UpsertAllocationRequest(
  @Schema(
    description = "Booking id of the prisoner",
    required = true,
    example = "1234567",
  )
  val bookingId: Long,

  @Schema(description = "The prisoner's pay band", example = "2")
  @field:NotBlank
  @field:Length(min = 1, max = 12)
  val payBandCode: String,

  @Schema(description = "Activity start date", required = true, example = "2022-08-12")
  val startDate: LocalDate,

  @Schema(description = "Activity end date", example = "2022-08-12")
  val endDate: LocalDate? = null,

  @Schema(description = "Activity end reason (from domain PS_END_RSN)", example = "REL")
  @field:Length(max = 12)
  val endReason: String? = null,

  @Schema(description = "Activity end comment")
  @field:Length(max = 240)
  val endComment: String? = null,

  @Schema(description = "Offender is suspended from Activity?")
  val suspended: Boolean? = false,

  @Schema(description = "Activity suspended comment")
  @field:Length(max = 240)
  val suspendedComment: String? = null,

  @Schema(description = "Offender program status from domain OFF_PRG_STS", example = "ALLOC")
  @field:NotBlank
  @field:Length(min = 1, max = 12)
  val programStatusCode: String,
)
