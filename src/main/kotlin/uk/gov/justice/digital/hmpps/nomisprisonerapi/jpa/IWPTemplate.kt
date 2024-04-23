package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.SequenceGenerator
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.type.YesNoConverter
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(name = "IWP_TEMPLATES")
class IWPTemplate(
  @Id
  @Column(name = "TEMPLATE_ID")
  @SequenceGenerator(name = "TEMPLATE_ID", sequenceName = "TEMPLATE_ID", allocationSize = 1)
  @GeneratedValue(generator = "TEMPLATE_ID")
  val id: Long = 0,

  @Column(name = "TEMPLATE_NAME")
  val name: String,

  @Column
  val description: String? = null,

  @Column
  val location: String? = null,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,

  @Lob
  @Column(name = "TEMPLATE_BODY", columnDefinition = "BLOB")
  val template: ByteArray? = null,

  @Column(name = "OBJECT_TYPE")
  val type: String? = null,

  @Column
  val expiryDate: LocalDate? = null,

  @Column(name = "LOCK_PASSWORD")
  val passwordLock: String? = null,

//  UNMAPPED FIELDS:
//  DATE_CREATED
//  USER_CREATED
//  MODIFY_DATETIME
//  MODIFY_USER_ID
//  AUDIT_TIMESTAMP
//  AUDIT_USER_ID
//  AUDIT_MODULE_NAME
//  AUDIT_CLIENT_USER_ID
//  AUDIT_CLIENT_IP_ADDRESS
//  AUDIT_CLIENT_WORKSTATION_NAME
//  AUDIT_ADDITIONAL_INFO
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  open lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  open lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as IWPTemplate
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
