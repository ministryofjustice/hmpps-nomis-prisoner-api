package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.BatchSize
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class SentenceId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "SENTENCE_SEQ", nullable = false)
  val sequence: Long
) : Serializable

@Entity
@Table(name = "OFFENDER_SENTENCES")
// Warning: on mandatory fields mapped - just enough to create test entities
data class OffenderSentence(
  @EmbeddedId
  val id: SentenceId,
  @OneToMany(mappedBy = "sentence", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val adjustments: MutableList<OffenderSentenceAdjustment> = mutableListOf(),

  @Column(name = "SENTENCE_STATUS")
  val status: String,
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "SENTENCE_CALC_TYPE",
        referencedColumnName = "SENTENCE_CALC_TYPE"
      ), JoinColumn(name = "SENTENCE_CATEGORY", referencedColumnName = "SENTENCE_CATEGORY")
    ]
  ) @BatchSize(size = 25)
  val calculationType: SentenceCalculationType,
  @Column(name = "START_DATE")
  val startDate: LocalDate,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderSentence
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
