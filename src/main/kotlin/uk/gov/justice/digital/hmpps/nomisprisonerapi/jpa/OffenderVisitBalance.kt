package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter

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

  @OneToMany(mappedBy = "visitBalance", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val visitBalanceAdjustments: MutableList<OffenderVisitBalanceAdjustment> = mutableListOf(),

  @Column(name = "REMAINING_VO")
  var remainingVisitOrders: Int? = null,

  @Column(name = "REMAINING_PVO")
  var remainingPrivilegedVisitOrders: Int? = null,

  @Column(name = "VISIT_ALLOWANCE_INDICATOR")
  @Convert(converter = YesNoConverter::class)
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
  override fun toString(): String = "${this::class.simpleName} (offenderBookingId = $offenderBookingId)"
}
