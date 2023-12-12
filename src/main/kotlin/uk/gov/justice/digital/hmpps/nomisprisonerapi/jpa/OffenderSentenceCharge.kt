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
import java.io.Serializable

@Embeddable
data class OffenderSentenceChargeId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "SENTENCE_SEQ", nullable = false)
  val sequence: Long,

  @Column(name = "OFFENDER_CHARGE_ID", nullable = false)
  val offenderChargeId: Long,
) : Serializable

@Entity
@Table(name = "OFFENDER_SENTENCE_CHARGES")
data class OffenderSentenceCharge(
  @EmbeddedId
  val id: OffenderSentenceChargeId,

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

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(
    name = "OFFENDER_CHARGE_ID",
    insertable = false,
    updatable = false,
  )
  val offenderCharge: OffenderCharge,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderSentenceCharge
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
