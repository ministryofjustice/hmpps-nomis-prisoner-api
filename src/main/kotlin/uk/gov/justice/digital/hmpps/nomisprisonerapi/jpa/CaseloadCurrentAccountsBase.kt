package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Objects

@Entity
@Table(name = "CASELOAD_CURRENT_ACCOUNTS_BASE")
data class CaseloadCurrentAccountsBase(
  @EmbeddedId
  val id: CaseloadCurrentAccountsBaseId,

  @MapsId("accountCode")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.RESTRICT)
  @JoinColumn(name = "ACCOUNT_CODE", nullable = false)
  val accountCode: AccountCode,

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @OnDelete(action = OnDeleteAction.RESTRICT)
  @JoinColumn(name = "ACCOUNT_PERIOD_ID", nullable = false)
  val accountPeriod: AccountPeriod,

  @NotNull
  @Column(name = "CURRENT_BALANCE", nullable = false, precision = 13, scale = 2)
  val currentBalance: BigDecimal,
) {
  @NotNull
  @Column(name = "MODIFY_DATE", nullable = false)
  val modifyDate: LocalDate = LocalDate.now()
}

@Embeddable
class CaseloadCurrentAccountsBaseId(
  @Size(max = 6)
  @NotNull
  @Column(name = "CASELOAD_ID", nullable = false, length = 6)
  val caseloadId: String,

  @NotNull
  @Column(name = "ACCOUNT_CODE", nullable = false)
  val accountCode: Int,
) {
  override fun hashCode(): Int = Objects.hash(caseloadId, accountCode)
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as CaseloadCurrentAccountsBaseId

    return caseloadId == other.caseloadId && accountCode == other.accountCode
  }
  override fun toString(): String = "CaseloadCurrentAccountsBaseId(caseloadId='$caseloadId', accountCode=$accountCode)"
}
