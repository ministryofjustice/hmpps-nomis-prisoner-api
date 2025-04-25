package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity
@Table(name = "EXTERNAL_SERVICES")
data class ExternalService(
  @Id
  @Column(nullable = false)
  val serviceName: String,

  @Column
  val description: String,

  @OneToMany(mappedBy = "id.externalService", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val serviceAgencySwitches: MutableList<ServiceAgencySwitch> = mutableListOf(),
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ExternalService
    return serviceName == other.serviceName
  }

  override fun hashCode(): Int = this.javaClass.hashCode()
}

const val VISIT_ALLOCATION_SERVICE = "VISIT_ALLOCATION"
