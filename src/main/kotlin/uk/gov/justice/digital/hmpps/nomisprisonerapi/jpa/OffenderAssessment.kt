package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Column
import jakarta.persistence.Converter
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csra.EvaluationResultCode
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
data class OffenderAssessmentId(
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "ASSESSMENT_SEQ", nullable = false)
  val sequence: Int,
) : Serializable

@Entity
@Table(name = "OFFENDER_ASSESSMENTS")
data class OffenderAssessment(
  @EmbeddedId
  val id: OffenderAssessmentId,

  val assessmentDate: LocalDate,

  @Column(name = "ASSESSMENT_TYPE_ID")
  val assessmentType: AssessmentType, // nullable but no null rows

  val score: BigDecimal, // nullable but no null rows

  @Column(name = "ASSESS_STATUS")
  @Enumerated(EnumType.STRING)
  val assessmentStatus: AssessmentStatusType,

  @Column(name = "CALC_SUP_LEVEL_TYPE")
  val calculatedLevel: AssessmentLevel? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ASSESS_STAFF_ID")
  val assessmentStaff: Staff,

  @Column(name = "ASSESS_COMMENT_TEXT")
  val assessmentComment: String? = null,

  @Column(name = "OVERRIDE_REASON_TEXT")
  val overrideReasonComment: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "PLACE_AGY_LOC_ID")
  val placementAgency: AgencyLocation? = null,

  @Column(name = "OVERRIDED_SUP_LEVEL_TYPE")
  val overrideLevel: AssessmentLevel? = null,

  @Column(name = "OVERRIDE_COMMENT_TEXT")
  val overrideComment: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OVERRIDE_STAFF_ID")
  val overrideStaff: Staff? = null,

  val evaluationDate: LocalDate? = null,
  val nextReviewDate: LocalDate? = null,

  @Enumerated(EnumType.STRING)
  val evaluationResultCode: EvaluationResultCode? = null,

  @Column(name = "REVIEW_SUP_LEVEL_TYPE")
  val reviewLevel: AssessmentLevel? = null,

  @Column(name = "REVIEW_PLACEMENT_TEXT")
  val reviewPlacementComment: String? = null,

  @Column(name = "REVIEW_COMMITTE_CODE")
  val reviewCommitteeCode: String? = null,

  @Column(name = "COMMITTE_COMMENT_TEXT")
  val reviewCommitteeComment: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "REVIEW_PLACE_AGY_LOC_ID")
  val reviewPlacementAgency: AgencyLocation? = null,

  @Column(name = "REVIEW_SUP_LEVEL_TEXT")
  val reviewComment: String? = null,

  @Column(name = "ASSESS_COMMITTE_CODE")
  val assessmentCommitteeCode: String? = null,

  @Column(name = "CREATION_DATE")
  val creationDateTime: LocalDateTime? = null,

  val creationUser: String? = null,

  @Column(name = "APPROVED_SUP_LEVEL_TYPE") // actually not used - all null for CSRA top level ***
  val approvedLevel: AssessmentLevel? = null,

  @Column(name = "ASSESSMENT_CREATE_LOCATION")
  val assessmentCreationLocation: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ASSESSOR_STAFF_ID")
  val assessorStaff: Staff? = null,

  val overrideUserId: String? = null,
  @Column(name = "OVERRIDE_REASON")
  val overrideReasonCode: String? = null, // 'PREVIOUS' or 'SECURITY', not sure if used
) : NomisAuditableEntityBasic()

enum class AssessmentStatusType { I, A, P }

enum class AssessmentType(val id: Int) {
  CSR(9687),    // CSR Rating
  CSR1(9684),   // CSR Reception
  CSRDO(9683),  // CSR Locate
  CSRF(9686),   // CSR Full
  CSRH(9685),   // CSR Health
  CSRREV(9682), // CSR Review
}

enum class AssessmentLevel { STANDARD, PEND, LOW, MED, HI }

@Converter(autoApply = true)
class CsraLevelConverter : AttributeConverter<AssessmentLevel?, String?> {
  override fun convertToDatabaseColumn(level: AssessmentLevel?): String? = level?.name

  override fun convertToEntityAttribute(level: String?): AssessmentLevel? = level
    ?.let { AssessmentLevel.entries.find { it.name == level } }
  // There are a handful of rows in prod with invalid levels such as 'Z', 'P' etc.
  // Here we are ignoring them and returning null
}

@Converter(autoApply = true)
class CsraTypeConverter : AttributeConverter<AssessmentType, Long> {
  override fun convertToDatabaseColumn(type: AssessmentType) = type.id.toLong()
  override fun convertToEntityAttribute(id: Long) = AssessmentType.entries.first { it.id.toLong() == id }
}
