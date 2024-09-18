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
import java.time.LocalDateTime

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
  open val phoneType: PhoneUsage,
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
  open var whenCreated: LocalDateTime = LocalDateTime.now()

  @Column(name = "MODIFY_DATETIME", nullable = false, insertable = false, updatable = false)
  open var whenModified: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Phone
    return phoneId == other.phoneId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
