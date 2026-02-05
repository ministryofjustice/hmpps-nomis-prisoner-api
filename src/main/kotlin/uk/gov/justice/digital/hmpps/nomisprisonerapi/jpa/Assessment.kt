package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.math.BigDecimal

@Entity
@Table(name = "ASSESSMENTS")
data class Assessment(
  @Id
  @Column(name = "ASSESSMENT_ID")
  val assessmentId: Long = 0,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "PARENT_ASSESSMENT_ID")
  val parentAssessment: Assessment? = null,

  @Column(name = "ASSESSMENT_CODE", nullable = false)
  val assessmentCode: String,

  @Column(name = "DESCRIPTION", nullable = false)
  val description: String,

  @Column(name = "LIST_SEQ")
  val listSequence: Int? = null,

  val score: BigDecimal? = null,

  @Column(name = "ASSESS_COMMENT")
  val comment: String? = null,

  @Column(name = "CELL_SHARING_ALERT_FLAG")
  @Convert(converter = YesNoConverter::class)
  val isCellSharing: Boolean = false,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Assessment
    return assessmentId == other.assessmentId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString() = "Assessment{assessmentId=$assessmentId, code=$assessmentCode}"
}
