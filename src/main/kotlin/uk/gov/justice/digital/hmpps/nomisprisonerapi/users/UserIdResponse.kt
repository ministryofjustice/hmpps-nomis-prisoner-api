package uk.gov.justice.digital.hmpps.nomisprisonerapi.users

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Staff user id")
data class UserIdResponse(
  @Schema(description = "The user id", required = true)
  val userId: Long,
)
