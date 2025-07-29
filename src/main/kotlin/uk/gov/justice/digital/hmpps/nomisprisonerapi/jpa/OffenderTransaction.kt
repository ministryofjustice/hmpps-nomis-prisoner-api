package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@EntityOpen
@IdClass(OffenderTransaction.Companion.Pk::class)
@Table(name = "OFFENDER_TRANSACTIONS")
data class OffenderTransaction(
  @Id
  @Column(name = "TXN_ID")
  val transactionId: Long = 0,

  @Id
  @Column(name = "TXN_ENTRY_SEQ")
  val transactionEntrySequence: Int,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking?,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(name = "CASELOAD_ID", referencedColumnName = "CASELOAD_ID", nullable = false),
      JoinColumn(name = "OFFENDER_ID", referencedColumnName = "OFFENDER_ID", nullable = false),
    ],
  )
  val trustAccount: OffenderTrustAccount,

  @Column(name = "SUB_ACCOUNT_TYPE", nullable = false)
  @Enumerated(EnumType.STRING)
  val subAccountType: SubAccountType,

  @ManyToOne(optional = false)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumn(name = "TXN_TYPE", nullable = false)
  val transactionType: TransactionType,

  @Column(name = "TXN_REFERENCE_NUMBER")
  val transactionReferenceNumber: String? = null,

  @Column(nullable = false)
  val modifyDate: LocalDateTime,

  @Column(name = "CLIENT_UNIQUE_REF")
  val clientUniqueRef: String? = null,

  // Date portion only
  @Column(name = "TXN_ENTRY_DATE", nullable = false)
  val entryDate: LocalDate,

  @Column(name = "TXN_ENTRY_DESC")
  val entryDescription: String? = null,

  @Column(name = "TXN_ENTRY_AMOUNT", nullable = false)
  val entryAmount: BigDecimal,

  @Column(name = "TXN_POSTING_TYPE", nullable = false)
  @Enumerated(EnumType.STRING)
  val postingType: PostingType,

  @OneToMany
  @JoinColumns(
    value = [
      JoinColumn(name = "TXN_ID", referencedColumnName = "TXN_ID", nullable = false, insertable = false, updatable = false),
      JoinColumn(name = "TXN_ENTRY_SEQ", referencedColumnName = "TXN_ENTRY_SEQ", nullable = false, insertable = false, updatable = false),
    ],
  )
  val generalLedgerTransactions: MutableList<GeneralLedgerTransaction> = mutableListOf(),
) : NomisAuditableEntityWithStaff() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderTransaction
    return transactionId == other.transactionId && transactionEntrySequence == other.transactionEntrySequence
  }

  override fun hashCode(): Int = javaClass.hashCode()

  companion object {
    data class Pk(
      val transactionId: Long? = null,
      val transactionEntrySequence: Int? = null,
    ) : Serializable
  }
}

enum class SubAccountType { REG, SAV, SPND, REL }
enum class PostingType { CR, DR }
