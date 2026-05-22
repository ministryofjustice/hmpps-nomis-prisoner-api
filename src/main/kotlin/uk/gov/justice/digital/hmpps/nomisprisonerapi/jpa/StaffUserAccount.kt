package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@EntityOpen
@Entity
@Table(name = "STAFF_USER_ACCOUNTS")
class StaffUserAccount(
  @Id
  @Column(name = "USERNAME", nullable = false)
  val username: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    unique = true,
    name = "USERNAME",
    referencedColumnName = "USERNAME",
    insertable = false,
    updatable = false,
  )
  val accountDetail: AccountDetail? = null,

  @ManyToOne
  @JoinColumn(name = "STAFF_ID", nullable = false)
  val staff: Staff,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + UserAccountType.USER_AC_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "STAFF_USER_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val type: UserAccountType,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + UserSourceType.ID_SOURCE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "ID_SOURCE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val source: UserSourceType,

  @Column(name = "WORKING_CASELOAD_ID")
  val activeCaseloadId: String? = null,

  @Column(name = "LAST_LOGON_DATE")
  var lastLoggedIn: LocalDateTime? = null,

  @OneToMany(mappedBy = "staffUserAccount", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val caseloads: MutableList<UserCaseload> = mutableListOf(),

) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as StaffUserAccount
    return username == other.username
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
