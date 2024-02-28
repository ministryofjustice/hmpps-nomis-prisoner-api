package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_SENTENCE_ADJUSTS")
class OffenderSentenceAdjustment(
  @SequenceGenerator(
    name = "OFFENDER_SENTENCE_ADJUST_ID",
    sequenceName = "OFFENDER_SENTENCE_ADJUST_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_SENTENCE_ADJUST_ID")
  @Id
  @Column(name = "OFFENDER_SENTENCE_ADJUST_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne
  @JoinColumn(name = "SENTENCE_ADJUST_CODE", nullable = false)
  var sentenceAdjustment: SentenceAdjustment,

  @Column(name = "ADJUST_DATE")
  var adjustmentDate: LocalDate?,

  @Column(name = "ADJUST_DAYS")
  var adjustmentNumberOfDays: Long,

  @Column(name = "ADJUST_FROM_DATE")
  var fromDate: LocalDate? = null,

  @Column(name = "ADJUST_TO_DATE")
  var toDate: LocalDate? = null,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "SENTENCE_SEQ", nullable = false)
  var sentenceSequence: Long,

  @Column(name = "OFFENDER_KEY_DATE_ADJUST_ID", nullable = true)
  val offenderKeyDateAdjustmentId: Long? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "OFFENDER_BOOK_ID",
        referencedColumnName = "OFFENDER_BOOK_ID",
        insertable = false,
        updatable = false,
      ),
      JoinColumn(
        name = "SENTENCE_SEQ",
        referencedColumnName = "SENTENCE_SEQ",
        insertable = false,
        updatable = false,
      ),
    ],
  )
  val sentence: OffenderSentence,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  var active: Boolean = false,

  @Column(name = "CREATE_DATETIME")
  val createdDate: LocalDateTime = LocalDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderSentenceAdjustment
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
