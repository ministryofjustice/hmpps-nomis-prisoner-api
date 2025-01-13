package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity
@Table(name = "CASELOADS")
class Caseload(
  // NB - only minimal columns have been mapped
  @Id
  @Column(name = "CASELOAD_ID", nullable = false)
  var id: String,
  @Column(name = "DESCRIPTION", nullable = false)
  val description: String,
) : NomisAuditableEntity() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Caseload

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
