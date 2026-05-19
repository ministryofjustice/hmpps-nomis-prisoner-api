package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable
import java.time.LocalDate
import java.util.Objects

@Embeddable
data class UserCaseloadId(
  @Column(name = "CASELOAD_ID", updatable = false, insertable = false)
  val caseloadId: String,

  @Column(name = "USERNAME", nullable = false)
  val username: String,
) : Serializable

@Entity
@Table(name = "USER_ACCESSIBLE_CASELOADS")
data class UserCaseload(
  @EmbeddedId
  val id: UserCaseloadId,

  @Column(name = "START_DATE")
  val startDate: LocalDate,

  @ManyToOne
  @MapsId("caseloadId")
  @JoinColumn(name = "CASELOAD_ID", updatable = false, insertable = false)
  val caseload: Caseload,
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
