package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Physical attributes upsert response")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpsertPhysicalAttributesResponse(
  @Schema(description = "Whether the record was created or updated")
  val created: Boolean,
  @Schema(description = "The offender booking ID that was changed")
  val bookingId: Long,
)
