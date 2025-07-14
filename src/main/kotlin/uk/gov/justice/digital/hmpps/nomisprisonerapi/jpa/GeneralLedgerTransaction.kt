package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@Entity
@EntityOpen
@IdClass(GeneralLedgerTransaction.Companion.Pk::class)
@Table(name = "GL_TRANSACTIONS")
data class GeneralLedgerTransaction(
  @Id
  @Column(name = "TXN_ID")
  val transactionId: Long = 0,

  @Id
  @Column(name = "TXN_ENTRY_SEQ")
  val transactionEntrySequence: Int,

  @Id
  @Column(name = "GL_ENTRY_SEQ")
  val generalLedgerEntrySequence: Int,

  @Column(nullable = false)
  val accountPeriodId: Long,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "ACCOUNT_CODE", nullable = false)
  val accountCode: AccountCode,

  @ManyToOne(optional = false)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumn(name = "TXN_TYPE", nullable = false)
  val transactionType: TransactionType,

  @Column(name = "TXN_POST_USAGE", nullable = false)
  @Enumerated(EnumType.STRING)
  val postUsage: PostingType,

  @Column(nullable = false)
  val caseloadId: String,

  @Column
  val offenderId: Long? = null,

  @Column(name = "TXN_REFERENCE_NUMBER")
  val transactionReferenceNumber: String? = null,

  @Column(name = "TXN_ENTRY_DATE", nullable = false)
  val entryDate: LocalDate,

  @Column(name = "TXN_ENTRY_TIME", nullable = false)
  val entryTime: LocalTime,

  @Column(name = "TXN_ENTRY_DESC")
  val entryDescription: String? = null,

  @Column(name = "TXN_ENTRY_AMOUNT", nullable = false)
  val entryAmount: BigDecimal,

  // A redundant copy of CREATE_DATETIME truncated to day but not nullable!
  @Column(nullable = false)
  val createDate: LocalDate,
) : NomisAuditableEntity() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as GeneralLedgerTransaction
    return transactionId == other.transactionId &&
      transactionEntrySequence == other.transactionEntrySequence &&
      generalLedgerEntrySequence == other.generalLedgerEntrySequence
  }

  override fun hashCode(): Int = javaClass.hashCode()

  companion object {
    data class Pk(
      val transactionId: Long? = null,
      val transactionEntrySequence: Int? = null,
      val generalLedgerEntrySequence: Int? = null,
    ) : Serializable
  }
}
