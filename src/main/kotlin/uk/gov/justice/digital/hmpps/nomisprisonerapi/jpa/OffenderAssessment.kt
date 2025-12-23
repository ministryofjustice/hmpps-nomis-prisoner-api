package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csra.EvaluationResultCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
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

@EntityOpen
@Entity
@Table(name = "OFFENDER_ASSESSMENTS")
data class OffenderAssessment(
  @EmbeddedId
  val id: OffenderAssessmentId,

  val assessmentDate: LocalDate,

  val assessmentTypeId: Long, // nullable but no null rows

  val score: BigDecimal, // nullable but no null rows

  @Column(name = "ASSESS_STATUS")
  @Enumerated(EnumType.STRING)
  val assessmentStatus: AssessmentStatusType,

  @Column(name = "CALC_SUP_LEVEL_TYPE")
  val calculatedLevel: String? = null,

  @Column(name = "ASSESS_STAFF_ID")
  val assessmentStaffId: Long,

  @Column(name = "ASSESS_COMMENT_TEXT")
  val assessmentComment: String? = null,

  @Column(name = "OVERRIDE_REASON_TEXT")
  val overrideReasonComment: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "PLACE_AGY_LOC_ID")
  val placementAgency: AgencyLocation? = null,

  @Column(name = "OVERRIDED_SUP_LEVEL_TYPE")
  val overrideLevel: String? = null,

  @Column(name = "OVERRIDE_COMMENT_TEXT")
  val overrideComment: String? = null,

  val overrideStaffId: Long? = null,

  val evaluationDate: LocalDate? = null,
  val nextReviewDate: LocalDate? = null,

  @Enumerated(EnumType.STRING)
  val evaluationResultCode: EvaluationResultCode? = null,

  @Column(name = "REVIEW_SUP_LEVEL_TYPE")
  val reviewLevel: String? = null,

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

  @Column(name = "APPROVED_SUP_LEVEL_TYPE")
  val approvedLevel: String? = null,

  @Column(name = "ASSESSMENT_CREATE_LOCATION")
  val assessmentCreationLocation: String? = null,

  val assessorStaffId: Long? = null,

  val overrideUserId: String? = null,
  @Column(name = "OVERRIDE_REASON")
  val overrideReasonCode: String? = null, // 'PREVIOUS' or 'SECURITY', not sure if used
) {
  @Column(name = "CREATE_DATETIME")
  @Generated
  lateinit var createDatetime: LocalDateTime

  @Column(name = "CREATE_USER_ID")
  @Generated
  lateinit var createUserId: String

  @Column(name = "MODIFY_DATETIME")
  @Generated
  var modifyDatetime: LocalDateTime? = null

  @Column(name = "MODIFY_USER_ID")
  @Generated
  var modifyUserId: String? = null

  @Column(name = "AUDIT_MODULE_NAME")
  @Generated
  var auditModuleName: String? = null
}

enum class AssessmentStatusType { I, A, P }

enum class AssessmentType { CSRF, CSRH, CSRDO, CSR, CSR1, CSRREV }
