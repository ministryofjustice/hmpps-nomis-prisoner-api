package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity
@Table(name = "STAFF_MEMBERS")
class Staff(
  @Id
  @Column(name = "STAFF_ID", nullable = false)
  val id: Long = 0,

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Staff
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
