package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Immutable
import java.io.Serializable
import java.util.Objects

@Embeddable
data class TransactionOperationId(
  @Column(name = "TXN_TYPE")
  val transactionType: String,
  @Column(name = "CASELOAD_ID")
  val caseloadId: String,
  @Column(name = "MODULE_NAME")
  val moduleName: String,
  @Column(name = "DR_ACCOUNT_CODE")
  val drAccountCode: Int?,
  @Column(name = "CR_ACCOUNT_CODE")
  val crAccountCode: Int?,
  @Column(name = "BANK_DR_ACCOUNT_CODE")
  val bankDrAccountCode: Int?,
) : Serializable

@Entity
@Immutable
@Table(name = "TRANSACTION_OPERATIONS")
class TransactionOperation(
  // There is no primary key on this table so have used the data to determine the primary key.
  @EmbeddedId
  val id: TransactionOperationId,

  @Column(name = "TXN_OPERATION_SEQ")
  val transactionOperationSequence: Int,

) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as TransactionOperation

    return id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  @Override
  override fun toString(): String = this::class.simpleName + "(EmbeddedId = $id)"
}
