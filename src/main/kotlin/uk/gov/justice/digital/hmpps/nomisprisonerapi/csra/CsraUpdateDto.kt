package uk.gov.justice.digital.hmpps.nomisprisonerapi.csra

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentCommittee
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentStatusType
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CsraUpdateDto(
  @Schema(description = "Status, active, inactive or provisional", allowableValues = ["I", "A", "P"], enumAsRef = true)
  val status: AssessmentStatusType,

  @Schema(description = "The assessment committee code (reference code in domain 'ASSESS_COMM')", enumAsRef = true)
  val committeeCode: AssessmentCommittee? = null,

  @Schema(description = "Next review date")
  val nextReviewDate: LocalDate? = null,

  @Schema(description = "Comment text")
  val comment: String? = null,

  @Schema(description = "A prison to be transferred to", example = "LEI")
  val placementAgencyId: String? = null,

  // Review fields:
  @Schema(description = "The review CSRA level", enumAsRef = true)
  val reviewLevel: AssessmentLevel? = null,

  @Schema(description = "The approval CSRA level", enumAsRef = true)
  val approvedLevel: AssessmentLevel? = null,

  @Schema(description = "Evaluation or approval date")
  val evaluationDate: LocalDate? = null,

  @Schema(description = "Approved or rejected indicator", enumAsRef = true)
  val evaluationResultCode: EvaluationResultCode? = null,

  @Schema(description = "The review/approval committee code (reference code in domain 'ASSESS_COMM')", enumAsRef = true)
  val reviewCommitteeCode: AssessmentCommittee? = null,

  @Schema(description = "Approval Committee Comment text")
  val reviewCommitteeComment: String? = null,

  @Schema(description = "The reviewed prison to be transferred to", example = "MDI")
  val reviewPlacementAgencyId: String? = null,

  @Schema(description = "Approval Comment text")
  val reviewComment: String? = null,
)
