package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Event id")
data class AppointmentIdResponse(
  @Schema(description = "The event id", required = true)
  val eventId: Long,
)
