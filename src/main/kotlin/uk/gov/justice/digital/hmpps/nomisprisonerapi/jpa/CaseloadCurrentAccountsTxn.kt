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
import org.hibernate.annotations.ColumnDefault
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "CASELOAD_CURRENT_ACCOUNTS_TXNS")
data class CaseloadCurrentAccountsTxn(
  @Id
  @Column(name = "CASELOAD_CURRENT_ACCOUNT_ID", nullable = false)
  @SequenceGenerator(name = "CASELOAD_CURRENT_ACCOUNT_ID", sequenceName = "CASELOAD_CURRENT_ACCOUNT_ID", allocationSize = 1)
  @GeneratedValue(generator = "CASELOAD_CURRENT_ACCOUNT_ID")
  val id: Long = 0,

  @Size(max = 6)
  @Column(name = "CASELOAD_ID", nullable = false, length = 6)
  val caseloadId: String,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ACCOUNT_CODE", nullable = false)
  val accountCode: AccountCode,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ACCOUNT_PERIOD_ID", nullable = false)
  val accountPeriod: AccountPeriod,

  @Column(name = "CURRENT_BALANCE", nullable = false, precision = 13, scale = 2)
  val currentBalance: BigDecimal,

  @ColumnDefault("systimestamp")
  @Column(name = "CREATE_DATETIME", nullable = false)
  val createDateTime: LocalDateTime = LocalDateTime.now(),
) {
  @Size(max = 32)
  @ColumnDefault("USER")
  @Column(name = "CREATE_USER_ID", nullable = false, length = 32)
  val createUserId: String = "OMSOWNER"

  @Column(name = "MODIFY_DATE", nullable = false)
  val modifyDate: LocalDate = LocalDate.now()
}
