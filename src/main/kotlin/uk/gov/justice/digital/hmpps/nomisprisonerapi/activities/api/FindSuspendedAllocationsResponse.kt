package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Find suspended prisoners from active allocations")
data class FindSuspendedAllocationsResponse(
  @Schema(description = "NOMIS offender number", example = "A1234BC")
  val offenderNo: String,
  @Schema(description = "Course Activity ID", example = "1234567")
  val courseActivityId: Long,
  @Schema(description = "Course description", example = "Kitchens AM")
  val courseActivityDescription: String,
)
