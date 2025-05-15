package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Move activity end date request")
data class MoveActivityEndDateRequest(
  @Schema(description = "Course activity ids", example = "[1, 2]")
  val courseActivityIds: Collection<Long>,
  @Schema(description = "Only update activities and allocation with this end date.", example = "2025-02-20")
  val oldEndDate: LocalDate,
  @Schema(description = "The new end date for activities and allocations", example = "2025-02-21")
  val newEndDate: LocalDate,
)
