package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Location deactivate request")
data class DeactivateRequest(

  @Schema(
    description = "The reason code for deactivation, reference data 'LIV_UN_RSN'",
    allowableValues = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"],
  )
  val reasonCode: String? = null,

  @Schema(description = "The expected reactivation date if any", example = "2024-12-31")
  val reactivateDate: LocalDate? = null,
)
