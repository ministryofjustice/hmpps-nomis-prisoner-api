package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.util.Objects

@Entity
@Table(name = "WORKS")
@EntityOpen
class Work(
  @Id
  @Column(name = "WORK_ID")
  val id: Long = 0,

  val workflowType: String,

  val workType: String,

  val workSubType: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Work
    return id == other.id
  }

  override fun hashCode(): Int {
    return Objects.hashCode(id)
  }
}
