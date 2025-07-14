package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime

@Embeddable
class OffenderTrustAccountId(
  @Column(name = "CASELOAD_ID", nullable = false, insertable = false, updatable = false)
  val caseloadId: String,

  @JoinColumn(name = "OFFENDER_ID", nullable = false)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val offender: Offender,
) : Serializable

@Entity
@EntityOpen
@Table(name = "OFFENDER_TRUST_ACCOUNTS")
data class OffenderTrustAccount(

  @EmbeddedId
  val id: OffenderTrustAccountId,

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
    return id.caseloadId == other.id.caseloadId && id.offender.id == other.id.offender.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  companion object {
    data class Pk(
      val caseloadId: String? = null,
      val offenderId: Long? = null,
    ) : Serializable
  }
}
