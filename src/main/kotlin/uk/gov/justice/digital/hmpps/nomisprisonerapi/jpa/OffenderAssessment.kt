package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.AttributeConverter
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Converter
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
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

  // Note: uses CsraTypeConverter
  @Column(name = "ASSESSMENT_TYPE_ID")
  val assessmentType: AssessmentType, // nullable but no null rows

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "ASSESSMENT_TYPE_ID", insertable = false, updatable = false)
  val assessment: Assessment? = null, // not nullable but dont want to have to specify this on creation

  val score: BigDecimal, // nullable but no null rows

  @Column(name = "ASSESS_STATUS")
  @Enumerated(EnumType.STRING)
  val assessmentStatus: AssessmentStatusType,

  @Column(name = "CALC_SUP_LEVEL_TYPE")
  val calculatedLevel: AssessmentLevel? = null,

  @ManyToOne(fetch = LAZY)
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

  @ManyToOne(fetch = LAZY)
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
  @Enumerated(EnumType.STRING)
  val reviewCommitteeCode: AssessmentCommittee? = null,

  @Column(name = "COMMITTE_COMMENT_TEXT")
  val reviewCommitteeComment: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "REVIEW_PLACE_AGY_LOC_ID")
  val reviewPlacementAgency: AgencyLocation? = null,

  @Column(name = "REVIEW_SUP_LEVEL_TEXT")
  val reviewComment: String? = null,

  @Column(name = "ASSESS_COMMITTE_CODE")
  @Enumerated(EnumType.STRING)
  val assessmentCommitteeCode: AssessmentCommittee? = null,

  @Column(name = "CREATION_DATE")
  val creationDateTime: LocalDateTime? = null,

  val creationUser: String? = null,

  // TODO: actually not used - all null for CSRA top level ***
  @Column(name = "APPROVED_SUP_LEVEL_TYPE")
  val approvedLevel: AssessmentLevel? = null,

  @Column(name = "ASSESSMENT_CREATE_LOCATION")
  val assessmentCreationLocation: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "ASSESSOR_STAFF_ID")
  val assessorStaff: Staff? = null,

  val overrideUserId: String? = null,

  @Column(name = "OVERRIDE_REASON")
  val overrideReasonCode: String? = null, // 'PREVIOUS' or 'SECURITY', not sure if used

  @OneToMany(fetch = LAZY, mappedBy = "offenderAssessment", cascade = [CascadeType.ALL], orphanRemoval = true)
  val offenderAssessmentItems: MutableList<OffenderAssessmentItem> = mutableListOf(),
) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderAssessment
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

enum class AssessmentStatusType { I, A, P }

enum class AssessmentType(val id: Long) {
  CSR(9687), // CSR Rating
  CSR1(9684), // CSR Reception
  CSRDO(9683), // CSR Locate
  CSRF(9686), // CSR Full
  CSRH(9685), // CSR Health
  CSRREV(9682), // CSR Review
}

enum class AssessmentLevel { STANDARD, PEND, LOW, MED, HI }

enum class AssessmentCommittee {
  GOV, // Governor
  MED, // Medical
  OCA, // OCA
  RECP, // Reception
  REVIEW, // Review Board
  SECSTATE, // Secretary of State
  SECUR, // Security
}

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
  override fun convertToDatabaseColumn(type: AssessmentType) = type.id
  override fun convertToEntityAttribute(id: Long) = AssessmentType.entries.first { it.id == id }
}
/* committee code usages :
794719	RECP
712447	RECP	RECP
439464	RECP	REVIEW
381756	REVIEW	REVIEW
272480	RECP	GOV
181961	REVIEW
105330
86605	REVIEW	GOV
73240		RECP
53548	RECP	SECUR
45130	GOV	GOV
37808	GOV
30300		REVIEW
28648	REVIEW	RECP
21889	GOV	REVIEW
15337		GOV
13158	GOV	RECP
12477	RECP	OCA
4059	OCA
3208	SECUR	SECUR
2707		SECUR
2619	REVIEW	SECUR
2487	MED
2445	SECUR
2102	SECUR	REVIEW
1617	OCA	OCA
1533	SECUR	GOV
1520	MED	REVIEW
990	REVIEW	OCA
898	OCA	GOV
838	OCA	REVIEW
800	SECUR	RECP
783	GOV	SECUR
751	MED	GOV
595	MED	MED
559	RECP	MED
501	OCA	RECP
456	GOV	OCA
420		OCA
400	REVIEW	MED
263	MED	RECP
101	SECUR	OCA
101	GOV	MED
59	OCA	SECUR
31		  MED
25	SECSTATE	REVIEW
20	MED	SECUR
17	SECSTATE	RECP
16	RECP	SECSTATE
14	SECSTATE
14	MED	OCA
3	SECSTATE	GOV

reference data: ASSESS_COMM:
GOV	Governor
MED	Medical
OCA	OCA
RECP	Reception
REVIEW	Review Board
SECSTATE	Secretary of State
SECUR	Security
 */
