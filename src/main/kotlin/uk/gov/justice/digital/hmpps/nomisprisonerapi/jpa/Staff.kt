package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "STAFF_MEMBERS")
class Staff(
  @Id
  @Column(name = "STAFF_ID", nullable = false)
  @SequenceGenerator(name = "STAFF_ID", sequenceName = "STAFF_ID", allocationSize = 1)
  @GeneratedValue(generator = "STAFF_ID")
  val id: Long = 0,

  @Column
  val firstName: String,

  @Column
  val lastName: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + StaffStatusType.STAFF_STATUS + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "STATUS",
          referencedColumnName = "code",
        ),
      ),
    ],
  )
  val status: StaffStatusType,

  @OneToMany(mappedBy = "staff", cascade = [CascadeType.ALL], orphanRemoval = true)
  // TODO do we want to move this logic to a subclass?
  @SQLRestriction("INTERNET_ADDRESS_CLASS = 'EMAIL'")
  val emails: MutableSet<StaffInternetAddress> = mutableSetOf(),

  @OneToMany(mappedBy = "staff", cascade = [CascadeType.ALL], orphanRemoval = true)
  val accounts: MutableList<StaffUserAccount> = mutableListOf(),

) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Staff
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
