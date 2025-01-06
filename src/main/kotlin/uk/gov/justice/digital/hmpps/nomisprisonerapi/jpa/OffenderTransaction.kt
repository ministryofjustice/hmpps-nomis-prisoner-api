package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
data class OffenderTransactionId(
  @SequenceGenerator(name = "TRANSACTION_ID", sequenceName = "TRANSACTION_ID", allocationSize = 1)
  @GeneratedValue(generator = "TRANSACTION_ID")
  @Column(name = "TXN_ID")
  val transactionId: Long,

  @Column(name = "TXN_ENTRY_SEQ")
  val transactionEntrySeq: Long,
) : Serializable

@Entity
@Table(name = "OFFENDER_TRANSACTIONS")
data class OffenderTransaction(
  @EmbeddedId
  val id: OffenderTransactionId,

  @Column(name = "CASELOAD_ID")
  val caseloadId: String,

  @Column(name = "OFFENDER_ID")
  val offenderId: Long,

  @Column(name = "TXN_POSTING_TYPE")
  val transactionPostingType: String = "CR",

  @Column(name = "TXN_TYPE")
  val transactionType: String = "A_EARN",

  @Column(name = "TXN_ENTRY_AMOUNT")
  var transactionEntryAmount: BigDecimal = BigDecimal.ZERO,

  @Column(name = "TXN_ENTRY_DATE")
  var transactionEntryDate: LocalDate = LocalDate.now(),

  @Column(name = "SUB_ACCOUNT_TYPE")
  val subAccountType: String = "SPND",

  @Column(name = "MODIFY_DATE")
  var modifyDate: LocalDate = LocalDate.now(),

  @Column(name = "SLIP_PRINTED_FLAG")
  var slipPrintedFlag: String = "Y",

  @OneToMany(mappedBy = "transaction", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val transactionDetails: MutableList<OffenderTransactionDetail> = mutableListOf(),
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderTransaction
    return id.transactionId == other.id.transactionId && id.transactionEntrySeq == other.id.transactionEntrySeq
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
