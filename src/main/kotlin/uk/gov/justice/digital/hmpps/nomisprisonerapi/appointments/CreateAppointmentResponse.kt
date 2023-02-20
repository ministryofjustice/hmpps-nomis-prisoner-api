package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Offender individual schedule creation response")
data class CreateAppointmentResponse(
  @Schema(description = "The created offender_ind_schedules id", required = true)
  val id: Long,
)
