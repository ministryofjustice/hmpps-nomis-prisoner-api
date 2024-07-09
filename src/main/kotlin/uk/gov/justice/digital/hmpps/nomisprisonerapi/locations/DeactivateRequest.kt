package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location deactivate request")
data class DeactivateRequest(

  @Schema(description = "The deactivation date, defaults to today", example = "2024-12-31")
  val deactivateDate: LocalDate? = null,

  @Schema(
    description = "The reason code for deactivation, reference data 'LIV_UN_RSN'",
    allowableValues = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"],
  )
  val reasonCode: String? = null,

  @Schema(description = "The expected reactivation date if any", example = "2024-12-31")
  val reactivateDate: LocalDate? = null,

  @Schema(description = "If true, update Nomis even if already inactive. Useful when a temporarily inactive location is deactivated permanently")
  val force: Boolean = false,
)
