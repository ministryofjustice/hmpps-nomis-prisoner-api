package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Course activity update request")
data class UpdateActivityRequest(

  @Schema(description = "Activity end date", example = "2022-08-12")
  val endDate: LocalDate?,

  @Schema(description = "Room where the activity is to occur (from activity schedule)")
  val internalLocationId: Long?,

  @Schema(description = "Pay rates", required = true)
  val payRates: List<PayRateRequest>,
)
