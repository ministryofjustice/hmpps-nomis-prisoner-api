package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.VISIT_ORDER_ADJUSTMENT
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_VISIT_BALANCE_ADJS")
data class OffenderVisitBalanceAdjustment(
  @SequenceGenerator(
    name = "OFFENDER_VISIT_BALANCE_ADJ_ID",
    sequenceName = "OFFENDER_VISIT_BALANCE_ADJ_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_VISIT_BALANCE_ADJ_ID")
  @Id
  @Column(name = "OFFENDER_VISIT_BALANCE_ADJ_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "REMAINING_VO")
  val remainingVisitOrders: Int? = null,

  @Column(name = "PREVIOUS_REMAINING_VO")
  val previousRemainingVisitOrders: Int? = null,

  @Column(name = "REMAINING_PVO")
  val remainingPrivilegedVisitOrders: Int? = null,

  @Column(name = "PREVIOUS_REMAINING_PVO")
  val previousRemainingPrivilegedVisitOrders: Int? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'$VISIT_ORDER_ADJUSTMENT'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "ADJUST_REASON_CODE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val adjustReasonCode: VisitOrderAdjustmentReason,

  @Column(name = "AUTHORISED_STAFF_ID")
  val authorisedStaffId: Long? = null,

  @Column(name = "ENDORSED_STAFF_ID")
  val endorsedStaffId: Long? = null,

  @Column(name = "ADJUST_DATE")
  val adjustDate: LocalDate,

  @Column(name = "COMMENT_TEXT")
  @Size(max = 240)
  val commentText: String? = null,

  @Column(name = "EXPIRY_BALANCE")
  val expiryBalance: Int? = null,

  @Column(name = "EXPIRY_DATE")
  val expiryDate: LocalDate? = null,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderVisitBalanceAdjustment
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
