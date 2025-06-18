package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable
import java.time.LocalDate

@Entity
@Table(name = "TRANSACTION_TYPES")
class TransactionType(
  @Id
  @Column(name = "TXN_TYPE", nullable = false)
  val type: String,

  @Column(name = "DESCRIPTION", nullable = false)
  val description: String,

  @Column(name = "TXN_USAGE", nullable = false)
  val transactionUsage: String, // Ref domain AC_TXN_USG

  @Column(nullable = false)
  val expiryDate: LocalDate,

  @Column(nullable = false)
  val modifyDate: LocalDate,

  // Other columns:
  // val caseloadType: String,
  //  ACTIVE_FLAG
  //  ALL_CASELOAD_FLAG
  //  UPDATE_ALLOWED_FLAG
  //  MANUAL_INVOICE_FLAG
  //  CREDIT_OBLIGATION_TYPE
  //  LIST_SEQ
  //  GROSS_NET_FLAG
  //  CASELOAD_TYPE
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as TransactionType
    return type == other.type
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
