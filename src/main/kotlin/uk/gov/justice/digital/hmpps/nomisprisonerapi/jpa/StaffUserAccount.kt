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
import java.time.LocalDateTime

@Entity
@Table(name = "STAFF_USER_ACCOUNTS")
class StaffUserAccount(
  @Id
  @Column(name = "USERNAME", nullable = false)
  val username: String,

  @ManyToOne
  @JoinColumn(name = "STAFF_ID", nullable = false)
  val staff: Staff,

  @Column(name = "STAFF_USER_TYPE", nullable = false)
  val type: String,

  @Column(name = "ID_SOURCE", nullable = false)
  val source: String,

  @Column(name = "WORKING_CASELOAD_ID")
  val activeCaseloadId: String? = null,

  @Column(name = "LAST_LOGON_DATE")
  var lastLoggedIn: LocalDateTime? = null,

  @OneToMany(mappedBy = "id.username", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
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
