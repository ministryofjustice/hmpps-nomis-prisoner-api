package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Incentive information")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AppointmentResponse(
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
)
