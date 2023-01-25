package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_KEY_DATE_ADJUSTS")
class OffenderKeyDateAdjustment(
  @SequenceGenerator(
    name = "OFFENDER_KEY_DATE_ADJUST_ID",
    sequenceName = "OFFENDER_KEY_DATE_ADJUST_ID",
    allocationSize = 1
  )
  @GeneratedValue(generator = "OFFENDER_KEY_DATE_ADJUST_ID")
  @Id
  @Column(name = "OFFENDER_KEY_DATE_ADJUST_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne
  @JoinColumn(name = "SENTENCE_ADJUST_CODE", nullable = false)
  val sentenceAdjustment: SentenceAdjustment,

  @Column(name = "ADJUST_DATE")
  val adjustmentDate: LocalDate,

  @Column(name = "ADJUST_DAYS")
  val adjustmentNumberOfDays: Long,

  @Column(name = "ADJUST_FROM_DATE")
  val fromDate: LocalDate? = null,

  @Column(name = "ADJUST_TO_DATE")
  val toDate: LocalDate? = null,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderKeyDateAdjustment
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
