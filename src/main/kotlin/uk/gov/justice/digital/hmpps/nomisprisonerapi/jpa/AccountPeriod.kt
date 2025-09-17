package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import java.time.LocalDate

@Entity
@Table(name = "ACCOUNT_PERIODS")
class AccountPeriod(
  @Id
  @Column(name = "ACCOUNT_PERIOD_ID", nullable = false)
  val id: Long,

  @Size(max = 12)
  @NotNull
  @Column(name = "ACCOUNT_PERIOD_TYPE", nullable = false, length = 12)
  val accountPeriodType: String,

  @Column(name = "START_DATE")
  val startDate: LocalDate,

  @Column(name = "END_DATE")
  val endDate: LocalDate,
) {
  // note - only mapped columns currently needed
  override fun hashCode(): Int = id.hashCode()
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    return id == (other as AccountPeriod).id
  }

  override fun toString(): String = "AccountPeriod(id=$id, accountPeriodType='$accountPeriodType', startDate=$startDate, endDate=$endDate)"
}
