package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "PHONES")
@DiscriminatorColumn(name = "OWNER_CLASS")
@Inheritance
abstract class Phone {
  @Id
  @SequenceGenerator(name = "PHONE_ID", sequenceName = "PHONE_ID", allocationSize = 1)
  @GeneratedValue(generator = "PHONE_ID")
  @Column(name = "PHONE_ID", nullable = false)
  open var phoneId: Long = 0

  @Column(name = "PHONE_TYPE")
  open var phoneType: String? = null

  @Column(name = "PHONE_NO")
  open var phoneNo: String? = null

  @Column(name = "EXT_NO")
  open var extNo: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Phone
    return phoneId == other.phoneId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
