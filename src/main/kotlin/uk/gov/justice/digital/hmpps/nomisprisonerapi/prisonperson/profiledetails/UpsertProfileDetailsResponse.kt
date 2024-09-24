package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.profiledetails

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Profile Details upsert response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpsertProfileDetailsResponse(
  @Schema(description = "Whether the record was created or updated")
  val created: Boolean,
  @Schema(description = "The offender booking ID that was changed")
  val bookingId: Long,
)
