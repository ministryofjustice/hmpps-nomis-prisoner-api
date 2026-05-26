package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDate
import java.util.Objects

@Embeddable
data class UserCaseloadId(
  @Column(name = "CASELOAD_ID")
  val caseloadId: String,
  @Column(name = "USERNAME")
  val username: String,
) : Serializable

@Entity
@EntityOpen
@Table(name = "USER_ACCESSIBLE_CASELOADS")
class UserCaseload(
  @EmbeddedId
  val id: UserCaseloadId,

  @Column(name = "START_DATE")
  val startDate: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CASELOAD_ID", updatable = false, insertable = false)
  val caseload: Caseload,

  @OneToMany(mappedBy = "userCaseload", cascade = [CascadeType.ALL], orphanRemoval = true)
  val roles: MutableList<UserCaseloadRole> = mutableListOf(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "USERNAME", updatable = false, insertable = false)
  val staffUserAccount: StaffUserAccount? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as UserCaseload

    return id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)

  @Override
  override fun toString(): String = this::class.simpleName + "(EmbeddedId = $id )"
}
