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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAssessmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class CsraService(
  private val offenderAssessmentRepository: OffenderAssessmentRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
) {
  fun createCsra(offenderNo: String, csraCreateRequest: CsraDto): CsraCreateResponse {
    val booking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Cannot find latest booking for offender $offenderNo")

    val placementAgency = csraCreateRequest.placementAgencyId?.let {
      agencyLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Cannot find placement agency $it")
    }

    val reviewPlacementAgency = csraCreateRequest.reviewPlacementAgencyId?.let {
      agencyLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Cannot find review placement agency $it")
    }

    val user = csraCreateRequest.createdBy?.let {
      staffUserAccountRepository.findByUsername(it)
        ?: throw BadDataException("Cannot find user $it")
    }
      ?: throw BadDataException("createdBy field is required")

    val sequence = offenderAssessmentRepository.getNextSequence(booking)
    val offenderAssessment = OffenderAssessment(
      OffenderAssessmentId(booking, sequence),
      calculatedLevel = csraCreateRequest.calculatedLevel,
      assessmentDate = csraCreateRequest.assessmentDate,
      assessmentType = AssessmentType.valueOf(csraCreateRequest.type.name),
      score = csraCreateRequest.score, // ?calculated by select s.MAX_SCORE from assessment_supervisions s where s.assessment_id = :assessmentTypeId and s.supervision_level_type = :category ?
      assessmentStatus = csraCreateRequest.status,
      assessmentStaff = user.staff,
      assessorStaff = user.staff,
      assessmentComment = csraCreateRequest.comment,
      nextReviewDate = csraCreateRequest.nextReviewDate,
      placementAgency = placementAgency,
      evaluationDate = csraCreateRequest.evaluationDate,
      evaluationResultCode = csraCreateRequest.evaluationResultCode,
      reviewLevel = csraCreateRequest.reviewLevel,
      reviewCommitteeCode = csraCreateRequest.reviewCommitteeCode,
      reviewCommitteeComment = csraCreateRequest.reviewCommitteeComment,
      reviewPlacementAgency = reviewPlacementAgency,
      reviewComment = csraCreateRequest.reviewComment,
      assessmentCommitteeCode = csraCreateRequest.committeeCode,
      approvedLevel = csraCreateRequest.approvedLevel,
      assessmentCreationLocation = booking.location.id,
      creationDateTime = csraCreateRequest.createdDateTime,
      creationUser = csraCreateRequest.createdBy,
      // TODO: not sure if needed yet:
      // reviewPlacementComment = csraCreateRequest.rev,
//      overrideReasonComment = csraCreateRequest.xxxx,
//      overrideLevel = csraCreateRequest.xxxx,
//      overrideComment = csraCreateRequest.xxxx,
//      overrideStaffId = csraCreateRequest.xxxx,
//      overrideUserId = csraCreateRequest.xxxx,
//      overrideReasonCode = csraCreateRequest.xxxx,
    )
    val saved = offenderAssessmentRepository.save(offenderAssessment)
    return CsraCreateResponse(saved.id.offenderBooking.bookingId, saved.id.sequence)
  }

  fun getCsra(bookingId: Long, sequence: Int): CsraDto {
    val booking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Booking with id $bookingId not found")

    return offenderAssessmentRepository.findByIdOrNull(
      OffenderAssessmentId(booking, sequence),
    )?.toDto()
      ?: throw NotFoundException("CSRA for booking $bookingId and sequence $sequence not found")
  }

  fun OffenderAssessment.toDto() = CsraDto(
    assessmentDate = assessmentDate,
    calculatedLevel = calculatedLevel,
    score = score,
    status = assessmentStatus,
    assessmentStaffId = assessmentStaff.id,
    type = assessmentType,
//      assessmentRepository.findByIdOrNull(assessmentTypeId)
//      ?.let { AssessmentType.valueOf(it.assessmentCode) }
//      ?: throw BadDataException("Cannot convert assessment type $assessmentTypeId for booking ${id.offenderBooking.bookingId} and sequence ${id.sequence}"),
    committeeCode = assessmentCommitteeCode,
    nextReviewDate = nextReviewDate,
    comment = assessmentComment,
    placementAgencyId = placementAgency?.id,
    createdDateTime = creationDateTime,
    createdBy = creationUser,
    reviewLevel = reviewLevel,
    approvedLevel = approvedLevel,
    evaluationDate = evaluationDate,
    evaluationResultCode = evaluationResultCode,
    reviewCommitteeCode = reviewCommitteeCode,
    reviewCommitteeComment = reviewCommitteeComment,
    reviewPlacementAgencyId = reviewPlacementAgency?.id,
    reviewComment = reviewComment,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CsraDto(
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CsraCreateResponse(
  @Schema(description = "The booking id", example = "2345678")
  val bookingId: Long,

  @Schema(description = "The sequence number of the assessment", example = "2")
  val sequence: Int,
)

enum class EvaluationResultCode { APP, REJ }
