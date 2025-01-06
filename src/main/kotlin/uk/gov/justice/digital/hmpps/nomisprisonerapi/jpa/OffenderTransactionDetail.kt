package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_TRANSACTION_DETAILS")
data class OffenderTransactionDetail(
  @SequenceGenerator(name = "TRANSACTION_DETAIL_ID", sequenceName = "TRANSACTION_DETAIL_ID", allocationSize = 1)
  @GeneratedValue(generator = "TRANSACTION_DETAIL_ID")
  @Id
  @Column(name = "TXN_DETAIL_ID", nullable = false)
  val transactionDetailId: Long,

  @ManyToOne
  @JoinColumns(
    value = [
      JoinColumn(
        name = "TXN_ID",
        referencedColumnName = "TXN_ID",
      ),
      JoinColumn(
        name = "TXN_ENTRY_SEQ",
        referencedColumnName = "TXN_ENTRY_SEQ",
      ),
    ],
  )
  val transaction: OffenderTransaction,

  @ManyToOne
  @JoinColumn(name = "EVENT_ID", nullable = true)
  val courseAttendance: OffenderCourseAttendance?,

  @Column(name = "CALENDAR_DATE", nullable = false)
  val calendarDate: LocalDate,

  @Column(name = "PAY_TYPE_CODE", nullable = false)
  val payTypeCode: String = "SESSION",

  @Column(name = "PAY_AMOUNT", nullable = false)
  var payAmount: BigDecimal,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderTransactionDetail
    return transactionDetailId == other.transactionDetailId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
