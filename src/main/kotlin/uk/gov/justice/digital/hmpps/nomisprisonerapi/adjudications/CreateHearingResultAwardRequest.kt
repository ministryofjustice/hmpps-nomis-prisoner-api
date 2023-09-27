package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A list of Hearing result awards (aka punishment) to be created")
data class CreateHearingResultAwardRequests(

  @Schema(description = "a list of award requests")
  val awardRequests: List<CreateHearingResultAwardRequest>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Hearing result award (aka punishment) to be created")
data class CreateHearingResultAwardRequest(
  @Schema(description = "The type of award", example = "CAUTION")
  val sanctionType: String,

  @Schema(description = "The status of the award", example = "IMMEDIATE")
  val sanctionStatus: String,

  @Schema(description = "Award comment", example = "GUILTY")
  val commentText: String?,

  @Schema(description = "Award effective date")
  val effectiveDate: LocalDate = LocalDate.now(),

  @Schema(description = "optional compensation amount", example = "0.50")
  val compensationAmount: BigDecimal?,

  @Schema(description = "the duration  of the award, in days")
  val sanctionDays: Int?,

  // TODO val consecutiveChargeNumber:
)
