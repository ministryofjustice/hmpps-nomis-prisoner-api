package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@EntityOpen
@Table(name = "OFFENDER_PAYMENT_PROFILES")
@DiscriminatorColumn(name = "PAYMENT_MODE")
@Inheritance
data class OffenderPaymentProfile(

  @Id
  @SequenceGenerator(name = "OFFENDER_PAYMENT_PROFILE_ID", sequenceName = "OFFENDER_PAYMENT_PROFILE_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_PAYMENT_PROFILE_ID")
  @Column(name = "OFFENDER_PAYMENT_PROFILE_ID", nullable = false, insertable = false, updatable = false)
  val id: Long = 0,

  @JoinColumn(name = "OFFENDER_ID")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val offender: Offender,

  @Column(nullable = false)
  val caseloadId: String,

  @Column(name = "TXN_TYPE", nullable = false)
  val transactionType: String,

  @Column(nullable = false)
  val startDate: LocalDate = LocalDate.now(),

  @Column
  val endDate: LocalDate? = null,

  @Column
  val paymentAmount: BigDecimal,

  @Column
  val referenceText: String? = null,

  @Column
  val commentText: String? = null,

  @Column
  val recordUserId: String? = null,

/*
  Not mapped:
  "AUDIT_TIMESTAMP" TIMESTAMP (9),
  "AUDIT_USER_ID" VARCHAR2(32 CHAR),
  "AUDIT_CLIENT_USER_ID" VARCHAR2(64 CHAR),
  "AUDIT_CLIENT_IP_ADDRESS" VARCHAR2(39 CHAR),
  "AUDIT_CLIENT_WORKSTATION_NAME" VARCHAR2(64 CHAR),
 */

) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderPaymentProfile

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
