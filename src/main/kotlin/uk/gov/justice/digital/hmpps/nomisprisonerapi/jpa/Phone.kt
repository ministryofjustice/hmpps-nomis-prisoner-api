package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
@Table(name = "PHONES")
@DiscriminatorColumn(name = "OWNER_CLASS")
@Inheritance
abstract class Phone(
  @Column(name = "PHONE_TYPE")
  open val phoneType: String,
  @Column(name = "PHONE_NO")
  open val phoneNo: String,
  @Column(name = "EXT_NO")
  open val extNo: String? = null,
) {
  @Id
  @SequenceGenerator(name = "PHONE_ID", sequenceName = "PHONE_ID", allocationSize = 1)
  @GeneratedValue(generator = "PHONE_ID")
  @Column(name = "PHONE_ID", nullable = false)
  open var phoneId: Long = 0

  @Column(name = "CREATE_DATETIME", nullable = false, insertable = false, updatable = false)
  var whenCreated: LocalDateTime = LocalDateTime.now()

  @Column(name = "MODIFY_DATETIME", nullable = false, insertable = false, updatable = false)
  var whenModified: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Phone
    return phoneId == other.phoneId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
