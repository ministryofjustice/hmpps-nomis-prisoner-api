package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Appointment counts")
data class AppointmentCountsResponse(
  @Schema(description = "The prison id")
  val prisonId: String,

  @Schema(description = "The event sub type")
  val eventSubType: String,

  @Schema(description = "Future appointments?")
  val future: Boolean,

  @Schema(description = "The count")
  val count: Long,
)
