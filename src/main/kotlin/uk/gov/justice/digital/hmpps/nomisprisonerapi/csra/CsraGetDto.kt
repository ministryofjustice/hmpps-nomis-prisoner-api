package uk.gov.justice.digital.hmpps.nomisprisonerapi.csra

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CsraGetDto(
  @Schema(description = "Date the CSRA was created", example = "2025-11-22")
  val assessmentDate: LocalDate,

  @Schema(
    description = """CSRA type as configured in the ASSESSMENTS table:
    CSRF	  CSR Full
    CSRH	  CSR Health
    CSRDO	  CSR Locate
    CSR	    CSR Rating
    CSR1	  CSR Reception
    CSRREV	CSR Review
  """,
    allowableValues = ["CSRF", "CSRH", "CSRDO", "CSR", "CSR1", "CSRREV"],
  )
  val type: AssessmentType,

  @Schema(description = "The calculated CSRA level", example = "STANDARD")
  val calculatedLevel: AssessmentLevel? = null,

  @Schema(description = "Score", example = "1000")
  val score: BigDecimal,

  @Schema(description = "Status, active, inactive or provisional", allowableValues = ["I", "A", "P"])
  val status: AssessmentStatusType,

  @Schema(description = "Staff id of user that created the CSRA", example = "123456")
  val assessmentStaffId: Long,

  @Schema(description = "The assessment committee code (reference code in domain 'ASSESS_COMM')")
  val committeeCode: String? = null,

  @Schema(description = "Next review date, defaults to current date + 6 months, if not provided")
  val nextReviewDate: LocalDate? = null,

  @Schema(description = "Comment text")
  val comment: String? = null,

  @Schema(description = "A prison to be transferred to", example = "LEI")
  val placementAgencyId: String? = null,

  @Schema(description = "Timestamp for when the CSRA was created", example = "2025-12-06T12:34:56")
  val createdDateTime: LocalDateTime? = null,

  @Schema(description = "The user who created the CSRA, required for CSRA creation", example = "NQP56Y")
  val createdBy: String? = null,

  // Review fields:
  @Schema(description = "The review CSRA level")
  val reviewLevel: AssessmentLevel? = null,

  @Schema(description = "The approval CSRA level")
  val approvedLevel: AssessmentLevel? = null,

  @Schema(description = "Evaluation or approval date")
  val evaluationDate: LocalDate? = null,

  @Schema(description = "Approved or rejected indicator")
  val evaluationResultCode: EvaluationResultCode? = null,

  @Schema(description = "The review/approval committee code (reference code in domain 'ASSESS_COMM')")
  val reviewCommitteeCode: String? = null,

  @Schema(description = "Approval Committee Comment text")
  val reviewCommitteeComment: String? = null,

  @Schema(description = "Approval Comment text")
  val reviewPlacementAgencyId: String? = null,

  @Schema(description = "Approval Comment text")
  val reviewComment: String? = null,
)
