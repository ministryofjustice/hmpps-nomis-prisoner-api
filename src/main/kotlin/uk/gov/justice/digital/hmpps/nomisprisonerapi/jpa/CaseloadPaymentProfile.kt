package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@EntityOpen
@Table(name = "CASELOAD_PAYMENT_PROFILES")
data class CaseloadPaymentProfile(
  @Id
  @SequenceGenerator(name = "CASELOAD_PAYMENT_PROFILE_ID", sequenceName = "CASELOAD_PAYMENT_PROFILE_ID", allocationSize = 1)
  @GeneratedValue(generator = "CASELOAD_PAYMENT_PROFILE_ID")
  @Column(name = "CASELOAD_PAYMENT_PROFILE_ID", nullable = false)
  val id: Long = 0,

  @Column(nullable = false)
  val caseloadId: String,

  @Column(name = "TXN_TYPE", nullable = false)
  val transactionType: String,

  @Column(nullable = false)
  val paymentMode: String,

  @Column
  val advanceAmount: BigDecimal? = null,

  @Column
  val paymentAmount: BigDecimal? = null,

  @Column(nullable = false)
  val weekDay: String,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = true,

  @Column
  val expiryDate: LocalDate? = null,

  @Column(nullable = false)
  val recordUserId: String,

  /*
    Not mapped:
    "AUDIT_TIMESTAMP" TIMESTAMP (9),
    "AUDIT_USER_ID" VARCHAR2(32 CHAR),
    "AUDIT_CLIENT_USER_ID" VARCHAR2(64 CHAR),
    "AUDIT_CLIENT_IP_ADDRESS" VARCHAR2(39 CHAR),
    "AUDIT_CLIENT_WORKSTATION_NAME" VARCHAR2(64 CHAR),
    "AUDIT_ADDITIONAL_INFO" VARCHAR2(256 CHAR),
   */

) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CaseloadPaymentProfile

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
