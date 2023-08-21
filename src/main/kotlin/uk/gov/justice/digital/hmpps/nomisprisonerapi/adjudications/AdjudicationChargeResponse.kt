package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "The requested adjudication charge and associated adjudication details. Note: the adjudication may have other charges associated with it")
data class AdjudicationChargeResponse(

  @Schema(
    description = "The adjudication/party sequence, part of the composite key with adjudicationIncidentId",
    required = true,
  )
  val adjudicationSequence: Int,

  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,

  @Schema(description = "The id of the booking associated with the adjudication", required = true)
  val bookingId: Long,

  @Schema(description = "The adjudication number (business key)")
  val adjudicationNumber: Long,

  @Schema(description = "Gender recorded in NOMIS")
  val gender: CodeDescription,

  @Schema(description = "Current prison or null if OUT")
  val currentPrison: CodeDescription?,

  @Schema(description = "Date Prisoner was added to the adjudication ????", required = true)
  val partyAddedDate: LocalDate,

  @Schema(description = "Adjudication comments")
  val comment: String? = null,

  @Schema(description = "Associated incident details")
  val incident: AdjudicationIncident,

  @Schema(description = "requested charge details.  Note that this adjudication may have other charges associated with it")
  val charge: AdjudicationCharge,

  @Schema(description = "Investigator that gathers evidence. Used in NOMIS in a small percentage of cases")
  val investigations: List<Investigation>,

  @Schema(description = "hearings associated with this adjudication")
  val hearings: List<Hearing>,
)
