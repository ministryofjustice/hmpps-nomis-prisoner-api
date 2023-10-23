package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A list of Hearing result awards (aka punishment) to be created")
data class CreateHearingResultAwardRequest(

  @Schema(description = "a list of award requests")
  val awardRequests: List<HearingResultAwardRequest>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A list of Hearing result awards (aka punishment) to be created and updated")
data class UpdateHearingResultAwardRequest(
  @Schema(description = "a list of award requests to create")
  val awardRequestsToCreate: List<HearingResultAwardRequest>,
  @Schema(description = "a list of award requests to update")
  val awardRequestsToUpdate: List<ExistingHearingResultAwardRequest>,
)

@Schema(description = "Hearing result award (aka punishment) to be created")
data class ExistingHearingResultAwardRequest(
  @Schema(description = "award to update")
  val awardRequests: HearingResultAwardRequest,
  @Schema(description = "sanction sequence for the booking associated with the adjudication")
  val sanctionSequence: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Hearing result award (aka punishment) to be created")
data class HearingResultAwardRequest(
  @Schema(
    description = "The type of award",
    example = "CAUTION",
    allowableValues = [
      "ADA",
      "CAUTION",
      "CC",
      "EXTRA_WORK",
      "EXTW",
      "FORFEIT",
      "OTHER",
      "REMACT",
      "REMWIN",
      "STOP_EARN",
      "STOP_PCT",
    ],
  )
  val sanctionType: String,

  @Schema(
    description = "The status of the award",
    example = "IMMEDIATE",
    allowableValues = [
      "AS_AWARDED",
      "AWARD_RED",
      "IMMEDIATE",
      "PROSPECTIVE",
      "QUASHED",
      "REDAPP",
      "SUSPENDED",
      "SUSPEN_EXT",
      "SUSPEN_RED",
      "SUSP_PROSP",
    ],
  )
  val sanctionStatus: String,

  @Schema(description = "Award comment", example = "GUILTY")
  val commentText: String?,

  @Schema(description = "Award effective date")
  val effectiveDate: LocalDate = LocalDate.now(),

  @Schema(description = "optional compensation amount", example = "0.50")
  val compensationAmount: BigDecimal?,

  @Schema(description = "the duration  of the award, in days")
  val sanctionDays: Int?,

  @Schema(description = "adjudication that contains the matching award that this award is consecutive to")
  val consecutiveCharge: AdjudicationChargeId?,
)

data class AdjudicationChargeId(
  @Schema(description = "adjudication number")
  val adjudicationNumber: Long,

  @Schema(description = "charge sequence within the adjudication")
  val chargeSequence: Int,
)
