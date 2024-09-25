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
import org.hibernate.annotations.Generated
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
class PersonEmploymentPK(
  @JoinColumn(name = "PERSON_ID", nullable = false)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val person: Person,

  @Column(name = "EMPLOYMENT_SEQ", nullable = false)
  val sequence: Long = 0,
) : Serializable

@Entity
@Table(name = "PERSON_EMPLOYMENTS")
class PersonEmployment(

  @EmbeddedId
  val id: PersonEmploymentPK,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = true,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "EMPLOYER_CORP_ID", nullable = true)
  val employerCorporate: Corporate?,

  /*
  Not mapped:
  OCCUPATION_CODE - always null
  EMPLOYER_NAME - always null
  HOURS_WEEK - always null
  SCHEDULE_TYPE - always null
  WAGE - always null
  WAGE_PERIOD_CODE - always null
  EMPLOYMENT_DATE - always null
  PHONE_EXT - always null
  PHONE_AREA - always null
  TERMINATION_DATE - always null
  COMMENT_TEXT - always null
  CONTACT_TYPE - always null
  CONTACT_NUMBER - always null
  PROV_STATE_CODE - always null
  CITY - always null
  ADDRESS_2 - always null
  ADDRESS_1 - always null
  SUPERVISOR_NAME - always null
   */
) {
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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as PersonEmployment

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
