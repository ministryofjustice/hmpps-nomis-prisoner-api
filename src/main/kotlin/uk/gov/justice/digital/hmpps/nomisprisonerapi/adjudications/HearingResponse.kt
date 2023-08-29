package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Hearing to be created")
data class HearingResponse(

  @Schema(description = "Type of hearing", example = "GOV")
  val hearingType: String,

  @Schema(description = "Hearing date")
  val hearingDate: LocalDate? = LocalDate.now(),

  @Schema(description = "Hearing time")
  val hearingTime: LocalTime? = LocalTime.now(),

  @Schema(description = "agency id of hearing")
  val agencyId: String,

  @Schema(description = "location id for the hearing", example = "123456")
  val internalLocationId: Long,

  @Schema(description = "associated adjudication number", example = "123456")
  val adjudicationNumber: Long,

  val comment: String? = null,

  @Schema(description = "Hearing staff Id")
  val hearingStaff: Staff? = null,
)
