package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_DEDUCTIONS")
data class OffenderDeduction(
  @Id
  @SequenceGenerator(name = "OFFENDER_DEDUCTION_ID", sequenceName = "DEDUCTION_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_DEDUCTION_ID")
  @Column(name = "OFFENDER_DEDUCTION_ID")
  val deductionId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_ID", nullable = false)
  val offender: Offender,

  @Column(name = "CREDIT_LIMIT")
  val creditLimit: BigDecimal? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(
    JoinColumn(name = "CASELOAD_ID", referencedColumnName = "CASELOAD_ID", nullable = false),
    JoinColumn(name = "DEDUCTION_TYPE", referencedColumnName = "DEDUCTION_TYPE", nullable = false),
  )
  val caseloadDeductionProfile: CaseloadDeductionProfile,

  @Column(name = "DEDUCTION_STATUS", nullable = false)
  val deductionStatus: String = "A",
  // Always 'A'

  @Column(name = "DEDUCTION_PRIORITY", nullable = false)
  val deductionPriority: Int,

  @Column(name = "INFORMATION_NUMBER")
  val informationNumber: String? = null,

  @Column(name = "DEDUCTION_PERCENTAGE")
  val deductionPercentage: Int? = null,

  @Column(name = "PROCESS_PRIORITY_NUMBER")
  val processPriorityNumber: Int? = 99,

  @Column(name = "EFFECTIVE_DATE", nullable = false)
  val effectiveDate: LocalDate,

  @Column(name = "COMMENT_TEXT")
  val commentText: String? = null,

  @Column(name = "FIFO_FLAG")
  @Convert(converter = YesNoConverter::class)
  val fifoFlag: Boolean = false,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PAYEE_PERSON_ID")
  val payeePerson: Person? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PAYEE_CORPORATE_ID")
  val payeeCorporate: Corporate? = null,

  @Column(name = "MAX_MONTHLY_AMOUNT")
  val maxMonthlyAmount: BigDecimal? = null,

  @Column(name = "MAX_TOTAL_AMOUNT")
  val maxTotalAmount: BigDecimal? = null,

  @Column(name = "DEDUCTION_AMOUNT")
  val deductionAmount: BigDecimal? = null,

  @Column(name = "ADJUSTMENT_REASON_CODE")
  val adjustmentReasonCode: String? = null,

  @Column(name = "ADJUSTMENT_AMOUNT")
  val adjustmentAmount: BigDecimal? = null,

  @Column(name = "ADJUSTMENT_USER_ID")
  val adjustmentUserId: String? = null,

  @Column(name = "ADJUSTMENT_TXN_ID")
  val adjustmentTransactionId: Long? = null,

  @Column(name = "ADJUSTMENT_TEXT")
  val adjustmentText: String? = null,

  @Column(name = "MODIFY_DATE", nullable = false)
  val modifyDate: LocalDateTime = LocalDateTime.now(),

  @Column(name = "PAY_DEDUCTION_FLAG")
  @Convert(converter = YesNoConverter::class)
  val payDeductionFlag: Boolean = false,

  @Column(name = "MAX_RECURSIVE_AMOUNT")
  val maxRecursiveAmount: BigDecimal? = null,

  @Column(name = "GROUP_ID")
  val groupId: Int? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CASE_ID")
  val courtCase: CourtCase? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_DEDUCTION_ID")
  val parentDeduction: OffenderDeduction? = null,

  @Column(name = "JS_STATUS")
  val jsStatus: String? = null,

  @Column(name = "COLLECT_AGENCY_AMOUNT")
  val collectAgencyAmount: BigDecimal? = null,

  @Column(name = "COLLECT_AGENCY_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val collectAgencyFlag: Boolean = false,

  @Column(name = "COLLECT_SENT_DATE")
  val collectSentDate: LocalDate? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_PAYMENT_PROFILE_ID")
  var offenderAdvance: OffenderAdvance? = null,

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
    other as OffenderDeduction

    return deductionId == other.deductionId
  }

  override fun hashCode(): Int = deductionId.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (deductionId = $deductionId )"
}
