package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
data class OffenderSentenceTermId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "SENTENCE_SEQ", nullable = false)
  val sentenceSequence: Long,

  // seq boundary is the sentence
  @Column(name = "TERM_SEQ", nullable = false)
  val termSequence: Long,
) : Serializable

@Entity
@Table(name = "OFFENDER_SENTENCE_TERMS")
data class OffenderSentenceTerm(
  @EmbeddedId
  val id: OffenderSentenceTermId,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(
    JoinColumn(
      name = "OFFENDER_BOOK_ID",
      referencedColumnName = "OFFENDER_BOOK_ID",
      insertable = false,
      updatable = false,
    ),
    JoinColumn(name = "SENTENCE_SEQ", referencedColumnName = "SENTENCE_SEQ", insertable = false, updatable = false),
  )
  val offenderSentence: OffenderSentence,

  @Column(name = "START_DATE")
  var startDate: LocalDate,

  @Column(name = "END_DATE")
  var endDate: LocalDate? = null,

  var years: Int? = null,
  var months: Int? = null,
  var weeks: Int? = null,
  var days: Int? = null,
  // all time portions used in prod
  var hours: Int? = null,

  // defaults to 'N' used in prod
  @Convert(converter = YesNoConverter::class)
  var lifeSentenceFlag: Boolean = false,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + SentenceTermType.SENT_TERM + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "SENTENCE_TERM_CODE", referencedColumnName = "code")),
    ],
  )
  var sentenceTermType: SentenceTermType,
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderSentenceTerm
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String = "OffenderSentenceTerm(termSequence=${id.termSequence}, sentenceSeq=${id.sentenceSequence}, bookingId=${id.offenderBooking.bookingId} startDate=$startDate, endDate=$endDate, years=$years, months=$months, weeks=$weeks, days=$days, hours=$hours, lifeSentenceFlag=$lifeSentenceFlag, sentenceTermType=$sentenceTermType)"
}
