package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.math.BigDecimal
import kotlin.jvm.javaClass

@Embeddable
class OffenderSubAccountId(
  @Column(name = "CASELOAD_ID", nullable = false, insertable = false, updatable = false)
  val caseloadId: String,

  @JoinColumn(name = "OFFENDER_ID", nullable = false)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val offender: Offender,

  @Column(name = "TRUST_ACCOUNT_CODE", nullable = false, insertable = false, updatable = false)
  val accountCode: Long,
)

@Entity
@EntityOpen
@Table(name = "OFFENDER_SUB_ACCOUNTS")
class OffenderSubAccount(

  @EmbeddedId
  val id: OffenderSubAccountId,

  @Column(nullable = false)
  val balance: BigDecimal,

  @Column(nullable = false)
  val holdBalance: BigDecimal,

  @Column(name = "LAST_TXN_ID", nullable = false)
  val lastTransactionId: Long,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderSubAccount
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
