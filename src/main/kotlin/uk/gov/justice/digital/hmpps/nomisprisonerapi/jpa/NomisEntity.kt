package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@MappedSuperclass
@EntityOpen
class NomisEntity {
  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "CREATE_USER_ID", insertable = false, updatable = false)
  var createStaffUserAccount: StaffUserAccount? = null

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  var modifyStaffUserAccount: StaffUserAccount? = null

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  @Generated
  var modifyUserId: String? = null

  @Column(name = "MODIFY_DATETIME", insertable = false, updatable = false)
  @Generated
  var modifyDatetime: LocalDateTime? = null

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
