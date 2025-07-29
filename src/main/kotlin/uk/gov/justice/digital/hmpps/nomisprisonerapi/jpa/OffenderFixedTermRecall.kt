package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate

@EntityOpen
@Entity
@Table(name = "OFFENDER_FIXED_TERM_RECALLS")
data class OffenderFixedTermRecall(
  @Id
  @Column(name = "OFFENDER_BOOK_ID", nullable = false)
  var id: Long? = null,

  @OneToOne
  @MapsId
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  var offenderBooking: OffenderBooking,

  @Column(name = "RETURN_TO_CUSTODY_DATE", nullable = false)
  var returnToCustodyDate: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "STAFF_ID", nullable = false)
  var staff: Staff,

  @Column(name = "COMMENT_TEXT", length = 4000)
  var comments: String? = null,

  @Column(name = "RECALL_LENGTH", nullable = false)
  var recallLength: Long = 28,
) : NomisAuditableEntityWithStaff() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderFixedTermRecall

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id )"
}
