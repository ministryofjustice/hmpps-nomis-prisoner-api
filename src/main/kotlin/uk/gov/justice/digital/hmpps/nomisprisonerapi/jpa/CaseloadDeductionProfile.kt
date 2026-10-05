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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Objects

@Embeddable
class CaseloadDeductionProfileId(
  @Column(name = "CASELOAD_ID", nullable = false)
  val caseloadId: String,

  @Column(name = "DEDUCTION_TYPE", nullable = false)
  val deductionType: String,
) {
  override fun hashCode(): Int = Objects.hash(caseloadId, deductionType)
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as CaseloadDeductionProfileId

    return caseloadId == other.caseloadId && deductionType == other.deductionType
  }
  override fun toString(): String = "CaseloadDeductionProfileId(caseloadId='$caseloadId', deductionType='$deductionType')"
}

@Entity
@EntityOpen
@Table(name = "CASELOAD_DEDUCTION_PROFILES")
data class CaseloadDeductionProfile(
  @EmbeddedId
  val id: CaseloadDeductionProfileId,

  @Column(name = "DELAY_RECAPTURE")
  val delayRecapture: Int? = null,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = true,

  @Column(name = "EFFECTIVE_DATE", nullable = false)
  val effectiveDate: LocalDate,

  @Column(name = "FIFO_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val fifoFlag: Boolean = true,

  @Column(name = "FO_AL_ALL_OFFENDER_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val allOffenderFlag: Boolean = false,

  @Column(name = "PERCENTAGE", nullable = false)
  val percentage: BigDecimal,

  @Column(name = "INTERNAL_PRIORITY_NO", nullable = false)
  val internalPriorityNumber: Int,

  @Column(name = "EXTERNAL_PRIORITY_NO", nullable = false)
  val externalPriorityNumber: Int,

  @Column(name = "ACCOUNT_CODE", nullable = false)
  val accountCode: Int,

  @Column(name = "CO_LIMIT_AMOUNT")
  val coLimitAmount: BigDecimal? = null,

  @Column(name = "CO_CREDIT_WHEN_INDIGENT_FLAG")
  @Convert(converter = YesNoConverter::class)
  val coCreditWhenIndigentFlag: Boolean = false,

  @Column(name = "MAX_MONTHLY_AMOUNT")
  val maxMonthlyAmount: BigDecimal? = null,

  @Column(name = "MAX_TOTAL_AMOUNT")
  val maxTotalAmount: BigDecimal? = null,

  @Column(name = "EXPIRY_DATE")
  val expiryDate: LocalDate? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PAYEE_PERSON_ID")
  val payeePerson: Person? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PAYEE_CORPORATE_ID")
  val payeeCorporate: Corporate? = null,

  @Column(name = "MODIFY_DATE", nullable = false)
  val modifyDate: LocalDate = LocalDate.now(),

  @Column(name = "LIST_SEQ")
  val listSequence: Int? = 99,

  @Column(name = "FLAT_RATE")
  val flatRate: BigDecimal? = null,

  @Column(name = "MINIMUM_TRUST_BALANCE")
  val minimumTrustBalance: BigDecimal? = null,

  @Column(name = "INDIGENT_MANDATORY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val indigentMandatoryFlag: Boolean = false,

  @Column(name = "COMM_CONDITION_TYPE")
  val commConditionType: String? = null,

  @Column(name = "COMM_CONDITION_CODE")
  val commConditionCode: String? = null,

  @Column(name = "MAX_RECURSIVE_AMOUNT")
  val maxRecursiveAmount: BigDecimal? = null,

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
    other as CaseloadDeductionProfile

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
