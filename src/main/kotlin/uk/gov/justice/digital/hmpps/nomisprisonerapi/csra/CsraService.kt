package uk.gov.justice.digital.hmpps.nomisprisonerapi.csra

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAssessmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

private const val CSRA_ASSSESSMENT_TYPE = 9687L
private const val TOP_LEVEL_ASSESSMENT_CLASS = "TYPE"

@Service
@Transactional
class CsraService(
  private val offenderAssessmentRepository: OffenderAssessmentRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val assessmentRepository: AssessmentRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
) {
  fun createCsra(offenderNo: String, csraCreateRequest: CsraCreateRequest): CsraCreateResponse {
    val booking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Cannot find latest booking for offender $offenderNo")

    val placementAgency = csraCreateRequest.placementAgencyId?.let {
      agencyLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Cannot find placement agency ${csraCreateRequest.placementAgencyId}")
    }

    val reviewPlacementAgency = csraCreateRequest.reviewPlacementAgencyId?.let {
      agencyLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Cannot find review placement agency ${csraCreateRequest.placementAgencyId}")
    }

    val assessment = assessmentRepository.findOneByAssessmentCodeAndAssessmentClass(
      csraCreateRequest.type.name,
      TOP_LEVEL_ASSESSMENT_CLASS,
    )
      ?: throw BadDataException("Cannot find assessment for code ${csraCreateRequest.type}")

    val user = staffUserAccountRepository.findByUsername(csraCreateRequest.createdBy)
      ?: throw BadDataException("Cannot find user ${csraCreateRequest.createdBy}")

    val sequence = offenderAssessmentRepository.getNextSequence(booking)
    val offenderAssessment = OffenderAssessment(
      OffenderAssessmentId(booking, sequence),
      calculatedLevel = csraCreateRequest.calculatedLevel,
      assessmentDate = csraCreateRequest.assessmentDate,
      assessmentTypeId = assessment.id,
      score = csraCreateRequest.score, // ?calculated by select s.MAX_SCORE from assessment_supervisions s where s.assessment_id = :assessmentTypeId and s.supervision_level_type = :category ?
      assessmentStatus = csraCreateRequest.status,
      assessmentStaffId = user.staff.id,
      assessorStaffId = user.staff.id,
      assessmentComment = csraCreateRequest.comment,
      nextReviewDate = csraCreateRequest.nextReviewDate,
//      overrideReasonComment = csraCreateRequest.xxxx,
      placementAgency = placementAgency,
//      overrideLevel = csraCreateRequest.xxxx,
//      overrideComment = csraCreateRequest.xxxx,
//      overrideStaffId = csraCreateRequest.xxxx,
      evaluationDate = csraCreateRequest.evaluationDate,
      evaluationResultCode = csraCreateRequest.evaluationResultCode,
      reviewLevel = csraCreateRequest.reviewLevel,
      // reviewPlacementComment = csraCreateRequest.rev,
      reviewCommitteeCode = csraCreateRequest.reviewCommitteeCode,
      reviewCommitteeComment = csraCreateRequest.reviewCommitteeComment,
      reviewPlacementAgency = reviewPlacementAgency,
      reviewComment = csraCreateRequest.reviewComment,
      assessmentCommitteeCode = csraCreateRequest.committeeCode,
      approvedLevel = csraCreateRequest.approvedLevel,
      assessmentCreationLocation = booking.location.id,
//      overrideUserId = csraCreateRequest.xxxx,
//      overrideReasonCode = csraCreateRequest.xxxx,
      creationDateTime = csraCreateRequest.createdDateTime,
      creationUser = csraCreateRequest.createdBy,
      /*
       CATs:
 Approve:
    createParams("bookingId", detail.getBookingId(),
                "seq", maxSequence,
                "assessmentTypeId", assessmentId,
                "assessStatus", "A",
                "category", detail.getCategory(),
                "evaluationDate", new SqlParameterValue(Types.DATE, DateTimeConverter.toDate(detail.getEvaluationDate())),
                "evaluationResultCode", "APP",
                "reviewCommitteeCode", detail.getReviewCommitteeCode(),
                "committeeCommentText", detail.getCommitteeCommentText(), // review
                "nextReviewDate", new SqlParameterValue(Types.DATE, DateTimeConverter.toDate(detail.getNextReviewDate())),
                "approvedCategoryComment", detail.getApprovedCategoryComment(),
                "approvedPlacementAgencyId", detail.getApprovedPlacementAgencyId(),
                "approvedPlacementText
    update OFFENDER_ASSESSMENTS set
        ASSESS_STATUS=:assessStatus,
        EVALUATION_DATE=:evaluationDate,
        EVALUATION_RESULT_CODE=:evaluationResultCode,
        REVIEW_SUP_LEVEL_TYPE=:category,
        REVIEW_SUP_LEVEL_TEXT=:approvedCategoryComment,
        REVIEW_COMMITTE_CODE=:reviewCommitteeCode,
        COMMITTE_COMMENT_TEXT=:committeeCommentText,
        NEXT_REVIEW_DATE=COALESCE(:nextReviewDate, NEXT_REVIEW_DATE),
        REVIEW_PLACE_AGY_LOC_ID=:approvedPlacementAgencyId,
        REVIEW_PLACEMENT_TEXT=:approvedPlacementText
      where OFFENDER_BOOK_ID=:bookingId
        and ASSESSMENT_SEQ=:seq
        and ASSESSMENT_TYPE_ID=:assessmentTypeId
        and ASSESS_STATUS='P'
Reject:
    createParams("bookingId", detail.getBookingId(),
                "seq", detail.getAssessmentSeq(),
                "assessmentTypeId", assessmentId,
                "evaluationDate", new SqlParameterValue(Types.DATE, DateTimeConverter.toDate(detail.getEvaluationDate())),
                "evaluationResultCode", "REJ",
                "reviewCommitteeCode", detail.getReviewCommitteeCode(),
                "committeeCommentText", detail.getCommitteeCommentText()
    update OFFENDER_ASSESSMENTS set
        EVALUATION_DATE=:evaluationDate,
        EVALUATION_RESULT_CODE=:evaluationResultCode,
        REVIEW_COMMITTE_CODE=:reviewCommitteeCode,
        COMMITTE_COMMENT_TEXT=:committeeCommentText
      where OFFENDER_BOOK_ID=:bookingId
        and ASSESSMENT_SEQ=:seq
        and ASSESSMENT_TYPE_ID=:assessmentTypeId
        and ASSESS_STATUS='P'
      */
    )
    offenderAssessmentRepository.save(offenderAssessment)
    return CsraCreateResponse(booking.bookingId, offenderAssessment.id.sequence)
  }
  /*
  NB assessment type could be:
    9686	CSRF	  CSR Full
    9685	CSRH	  CSR Health
    9683	CSRDO	  CSR Locate
    9687	CSR	    CSR Rating
    9684	CSR1	  CSR Reception
    9682	CSRREV	CSR Review
   */

  fun getCsra(bookingId: Long, sequence: Int): CsraDto {
    val booking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Booking with id $bookingId not found")

    return offenderAssessmentRepository.findByIdAndAssessmentTypeId(
      OffenderAssessmentId(booking, sequence),
      CSRA_ASSSESSMENT_TYPE,
    )?.toDto()
      ?: throw NotFoundException("CSRA for booking $bookingId and sequence $sequence not found")
  }
}

fun OffenderAssessment.toDto() = CsraDto(
  bookingId = id.offenderBooking.bookingId,
  sequence = id.sequence,
  assessmentDate = assessmentDate,
  calculatedLevel = calculatedLevel,
  score = score,
  status = assessmentStatus,
  assessmentStaffId = assessmentStaffId,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CsraDto(
  @Schema(description = "The booking id", example = "2345678")
  val bookingId: Long,

  @Schema(description = "The sequence number of the assessment", example = "2")
  val sequence: Int,

  @Schema(description = "Date the CSRA was created", example = "2025-11-22")
  val assessmentDate: LocalDate,

  @Schema(description = "The calculated CSRA level", example = "STANDARD")
  val calculatedLevel: String? = null,

  @Schema(description = "Score", example = "1000")
  val score: BigDecimal,

  @Schema(description = "Status, active, inactive or provisional", allowableValues = ["I", "A", "P"])
  val status: AssessmentStatusType,

  @Schema(description = "Staff id of user that created the CSRA", example = "123456")
  val assessmentStaffId: Long,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CsraCreateRequest(
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
  val calculatedLevel: String,

  @Schema(description = "Score", example = "1000")
  val score: BigDecimal,

  @Schema(description = "Status, active, inactive or provisional", allowableValues = ["I", "A", "P"])
  val status: AssessmentStatusType,

  @Schema(description = "Staff id of user that created the CSRA", example = "123456")
  val assessmentStaffId: Long,

  @Schema(description = "The assessment committee code (reference code in domain 'ASSESS_COMM')")
  val committeeCode: String,

  @Schema(description = "Next review date, defaults to current date + 6 months, if not provided")
  val nextReviewDate: LocalDate? = null,

  @Schema(description = "Comment text")
  val comment: String? = null,

  @Schema(description = "A prison to be transferred to", example = "LEI")
  val placementAgencyId: String? = null,

  @Schema(description = "Timestamp for when the CSRA was created", example = "2025-12-06T12:34:56")
  val createdDateTime: LocalDateTime,

  @Schema(description = "The user who created the CSRA", example = "NQP56Y")
  val createdBy: String,

  // Review fields:
  @Schema(description = "The review CSRA level")
  val reviewLevel: String? = null,

  @Schema(description = "The approval CSRA level")
  val approvedLevel: String? = null,

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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CsraCreateResponse(
  @Schema(description = "The booking id", example = "2345678")
  val bookingId: Long,

  @Schema(description = "The sequence number of the assessment", example = "2")
  val sequence: Int,
)

enum class EvaluationResultCode { APP, REJ }
