package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@EntityOpen
@IdClass(OffenderTrustAccount.Companion.Pk::class)
@Table(name = "OFFENDER_TRUST_ACCOUNTS")
data class OffenderTrustAccount(
  @Id
  @Column(name = "CASELOAD_ID", nullable = false, insertable = false, updatable = false)
  val prisonId: String,

  @Id
  @Column(name = "OFFENDER_ID", nullable = false, insertable = false, updatable = false)
  val offenderId: Long,

  @Column(name = "ACCOUNT_CLOSED_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val accountClosed: Boolean,

  val holdBalance: BigDecimal? = null,
  val currentBalance: BigDecimal? = null,

  @Column(nullable = false)
  val modifyDate: LocalDateTime,

  // Always 99! :
  //  @Column(name = "LIST_SEQ")
  //  val listSequence: Int? = null,

  // no longer used (null recently):
  // val notifyDate: LocalDateTime? = null,
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderTrustAccount
    return prisonId == other.prisonId && offenderId == other.offenderId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  companion object {
    data class Pk(
      val prisonId: String? = null,
      val offenderId: Long? = null,
    ) : Serializable
  }
}
