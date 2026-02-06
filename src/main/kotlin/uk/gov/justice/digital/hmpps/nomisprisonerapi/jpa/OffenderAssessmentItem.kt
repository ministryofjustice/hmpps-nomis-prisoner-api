package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Persistable
import java.io.Serializable
import java.math.BigDecimal

@Embeddable
data class OffenderAssessmentItemId(
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "ASSESSMENT_SEQ", nullable = false)
  val sequence: Int,

  @Column(name = "ITEM_SEQ", nullable = false)
  val itemSequence: Int,
) : Serializable

@Entity
@Table(name = "OFFENDER_ASSESSMENT_ITEMS")
data class OffenderAssessmentItem(
  @EmbeddedId
  private val id: OffenderAssessmentItemId,

  val score: BigDecimal,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "ASSESSMENT_ID")
  val assessment: Assessment,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "PARENT_ASSESSMENT_ID")
  val parentAssessment: Assessment? = null,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "OFFENDER_BOOK_ID",
          referencedColumnName = "OFFENDER_BOOK_ID",
          insertable = false,
          updatable = false,
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "ASSESSMENT_SEQ",
          referencedColumnName = "ASSESSMENT_SEQ",
          insertable = false,
          updatable = false,
        ),
      ),
    ],
  )
  val offenderAssessment: OffenderAssessment,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Transient
  @Value("false")
  val new: Boolean = true,
) : NomisAuditableEntityBasic(),
  Persistable<OffenderAssessmentItemId> {
  override fun getId() = id
  override fun isNew() = new

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderAssessmentItem
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString() = "Item(bookingId=${id.offenderBooking.bookingId}, seq=${id.sequence}," +
    " itemSeq=${id.itemSequence}, assessment=$assessment, comment=$comment)"
}
