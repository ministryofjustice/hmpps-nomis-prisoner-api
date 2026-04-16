package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable

@Embeddable
data class OffenderIndScheduleSentencesId(
  @Column(name = "EVENT_ID", nullable = false)
  val eventId: Long,

  @Column(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBookId: Long,

  @Column(name = "SENTENCE_SEQ", nullable = false)
  val sentenceSeq: Long,
) : Serializable

@Entity
@Table(name = "OFFENDER_IND_SCH_SENTS")
@EntityOpen
class OffenderIndScheduleSentence(
  @EmbeddedId
  val id: OffenderIndScheduleSentencesId,

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

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderIndScheduleSentence
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
