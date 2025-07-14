package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen

@Entity
@EntityOpen
@Table(name = "ACCOUNT_CODES")
data class AccountCode(
  @Id
  @Column(nullable = false, insertable = false, updatable = false)
  val accountCode: Int,

  @Column(nullable = false)
  val accountName: String,

  @Column(name = "POSTING_STATUS_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val postingStatus: Boolean,

  @Enumerated(EnumType.STRING)
  val subAccountType: SubAccountType? = null,

  val caseloadType: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AccountCode
    return accountCode == other.accountCode
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
