package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Hearing update fields")
data class UpdateHearingRequest(

  @Schema(description = "Type of hearing", example = "GOV")
  val hearingType: String,

  @Schema(description = "Hearing date")
  val hearingDate: LocalDate,

  @Schema(description = "Hearing time")
  val hearingTime: LocalTime,

  @Schema(description = "location id for the hearing", example = "123456")
  val internalLocationId: Long,
)
