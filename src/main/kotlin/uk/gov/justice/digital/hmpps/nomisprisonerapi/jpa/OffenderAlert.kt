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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
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

  @Column(name = "ALERT_DATE", nullable = false)
  val alertDate: LocalDate,

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

  @OneToOne(mappedBy = "alert", cascade = [CascadeType.ALL])
  var workFlow: AlertWorkFlow? = null,

  // @Column(name = "CASELOAD_ID")  not used, always null
  // @Column(name = "CREATE_DATE") always null not used
) {
  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  var modifyUserId: String? = null

  @Column(name = "MODIFY_DATETIME", insertable = false, updatable = false)
  var modifyDatetime: LocalDateTime? = null

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  lateinit var createDatetime: LocalDateTime

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

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
}

enum class AlertStatus {
  ACTIVE,
  INACTIVE,
}
