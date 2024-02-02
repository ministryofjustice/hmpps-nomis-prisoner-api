package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Summary of adjudication for a booking")
data class AdjudicationADAAwardSummaryResponse(
  @Schema(
    description = "Booking id for the summary",
    required = true,
  )
  val bookingId: Long,
  @Schema(
    description = "Prisoner number related to booking",
    required = true,
  )
  val offenderNo: String,
  @Schema(
    description = "List of ADAs awarded during this booking period",
    required = true,
  )
  val adaSummaries: List<ADASummary> = emptyList(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "ADA summary")
data class ADASummary(
  @Schema(
    description = "Parent adjudication number that lead to this award",
    required = true,
  )
  val adjudicationNumber: Long,
  @Schema(
    description = "Key to this sanction",
    required = true,
  )
  val sanctionSequence: Int,
  @Schema(
    description = "Number of days awards",
    required = true,
  )
  val days: Int,
  @Schema(
    description = "Date of award",
    required = true,
  )
  val effectiveDate: LocalDate,
  @Schema(
    description = "ADA status",
    required = true,
  )
  val sanctionStatus: CodeDescription,
)
