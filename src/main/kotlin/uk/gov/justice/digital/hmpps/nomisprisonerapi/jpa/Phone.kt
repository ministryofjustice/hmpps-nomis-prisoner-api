package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula

@Entity
@Table(name = "PHONES")
@DiscriminatorColumn(name = "OWNER_CLASS")
@Inheritance
abstract class Phone(
  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + PhoneUsage.PHONE_USAGE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "PHONE_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  open var phoneType: PhoneUsage,
  @Column(name = "PHONE_NO")
  open var phoneNo: String,
  @Column(name = "EXT_NO")
  open var extNo: String? = null,
) : NomisAuditableEntity() {
  @Id
  @SequenceGenerator(name = "PHONE_ID", sequenceName = "PHONE_ID", allocationSize = 1)
  @GeneratedValue(generator = "PHONE_ID")
  @Column(name = "PHONE_ID", nullable = false)
  var phoneId: Long = 0

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Phone
    return phoneId == other.phoneId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
