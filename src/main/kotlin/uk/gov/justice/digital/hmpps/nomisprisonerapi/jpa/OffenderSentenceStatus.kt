package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen

@Entity
@Table(name = "OFFENDER_SENTENCE_STATUSES")
@EntityOpen
class OffenderSentenceStatus(

  @SequenceGenerator(
    name = "OFFENDER_SENTENCE_STATUS_ID",
    sequenceName = "OFFENDER_SENTENCE_STATUS_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_SENTENCE_STATUS_ID")
  @Id
  @Column(name = "OFFENDER_SENTENCE_STATUS_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "SENTENCE_SEQ", nullable = false)
  val sequence: Long,

  val statusUpdateReason: String,

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

  @Column(name = "STATUS_UPDATE_STAFF_ID")
  val staffId: Long,

  // this class is only mapped to allow a cascading delete from OFFENDER_CASES -> OFFENDER_SENTENCES
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderSentenceStatus
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
