package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowStatus.DONE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
data class OffenderAlertId(
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "ALERT_SEQ", nullable = false)
  val sequence: Long,
) : Serializable

@Entity
@EntityOpen
@Table(name = "OFFENDER_ALERTS")
class OffenderAlert(
  @EmbeddedId
  val id: OffenderAlertId,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "ROOT_OFFENDER_ID")
  var rootOffender: Offender? = null,

  @Column(name = "ALERT_DATE", nullable = false)
  var alertDate: LocalDate,

  @Column(name = "EXPIRY_DATE")
  var expiryDate: LocalDate? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AlertType.DOMAIN + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "ALERT_TYPE", referencedColumnName = "code")),
    ],
  )
  val alertType: AlertType,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AlertCode.DOMAIN + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "ALERT_CODE", referencedColumnName = "code")),
    ],
  )
  val alertCode: AlertCode,

  @Column(name = "AUTHORIZE_PERSON_TEXT")
  var authorizePersonText: String? = null,

  @Enumerated(STRING)
  @Column(name = "ALERT_STATUS", nullable = false)
  var alertStatus: AlertStatus = ACTIVE,

  @Convert(converter = YesNoConverter::class)
  @Column(name = "VERIFIED_FLAG", nullable = false)
  val verifiedFlag: Boolean = false,

  @Column(name = "COMMENT_TEXT")
  var commentText: String? = null,

  @Column(name = "CASELOAD_TYPE")
  val caseloadType: String = "INST",

  // this is more or less a one to one but there are handful alerts with
  // no workflows and some with multiple workflows
  @OneToMany(mappedBy = "alert", fetch = LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  var workFlows: MutableList<AlertWorkFlow> = mutableListOf(),

  @Column(name = "CREATE_USER_ID")
  var createUsername: String,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "CREATE_USER_ID", insertable = false, updatable = false)
  val createStaffUserAccount: StaffUserAccount? = null,

  @Column(name = "MODIFY_USER_ID")
  var modifyUserId: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  val modifyStaffUserAccount: StaffUserAccount? = null,

  @Column(name = "MODIFY_DATETIME")
  var modifyDatetime: LocalDateTime? = null,
  // @Column(name = "CASELOAD_ID")  not used, always null
  // @Column(name = "CREATE_DATE") always null not used
) {

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  @Column(name = "AUDIT_TIMESTAMP", insertable = false, updatable = false)
  @Generated
  var auditTimestamp: LocalDateTime? = null

  @Column(name = "AUDIT_USER_ID", insertable = false, updatable = false)
  @Generated
  var auditUserId: String? = null

  @Column(name = "AUDIT_MODULE_NAME", insertable = false, updatable = false)
  @Generated
  var auditModuleName: String? = null

  @Column(name = "AUDIT_CLIENT_USER_ID", insertable = false, updatable = false)
  @Generated
  var auditClientUserId: String? = null

  @Column(name = "AUDIT_CLIENT_IP_ADDRESS", insertable = false, updatable = false)
  @Generated
  var auditClientIpAddress: String? = null

  @Column(name = "AUDIT_CLIENT_WORKSTATION_NAME", insertable = false, updatable = false)
  @Generated
  var auditClientWorkstationName: String? = null

  @Column(name = "AUDIT_ADDITIONAL_INFO", insertable = false, updatable = false)
  @Generated
  var auditAdditionalInfo: String? = null

  fun addWorkFlowLog(workActionCode: WorkFlowAction, workFlowStatus: WorkFlowStatus = DONE, createUsername: String? = null) {
    if (this.workFlows.isEmpty()) this.workFlows.add(AlertWorkFlow(this))
    val workFlow = this.workFlows.first()
    workFlow.logs.add(
      WorkFlowLog(
        id = WorkFlowLogId(workFlow, workFlow.nextSequence()),
        workActionCode = workActionCode,
        workFlowStatus = workFlowStatus,
        workActionDate = LocalDateTime.now(),
        createUsername = createUsername ?: workFlow.createUsername,
      ),
    )
  }
}

enum class AlertStatus {
  ACTIVE,
  INACTIVE,
}
