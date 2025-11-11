package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
data class OffenderProfileId(
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "PROFILE_SEQ", nullable = false)
  val sequence: Int,
) : Serializable

@Entity
@Table(name = "OFFENDER_PROFILES")
data class OffenderProfile(
  @EmbeddedId
  val id: OffenderProfileId,

  @Column(name = "CHECK_DATE")
  var checkDate: LocalDateTime,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderProfile
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(offenderBookingId = ${id.offenderBooking.bookingId}, sequence = $id.sequence)"
}
