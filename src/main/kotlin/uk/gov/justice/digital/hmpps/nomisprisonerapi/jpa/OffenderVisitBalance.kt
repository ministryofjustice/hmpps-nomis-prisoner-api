package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.MapsId
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "OFFENDER_VISIT_BALANCES")
data class OffenderVisitBalance(

  @Id
  @Column(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBookingId: Long = 0,

  @OneToOne
  @MapsId
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking,

  @Column(name = "REMAINING_VO")
  var remainingVisitOrders: Int? = null,

  @Column(name = "REMAINING_PVO")
  var remainingPrivilegedVisitOrders: Int? = null,

  @Column(name = "VISIT_ALLOWANCE_INDICATOR")
  val visitAllowanceIndicator: Boolean? = false,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderVisitBalance

    return offenderBookingId == other.offenderBookingId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(offenderBookingId = $offenderBookingId )"
  }
}
