package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable

@Embeddable
data class ServiceAgencySwitchId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "SERVICE_NAME", nullable = false)
  val externalService: ExternalService,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val agencyLocation: AgencyLocation,
) : Serializable

@Entity
@Table(name = "SERVICE_AGENCY_SWITCHES")
data class ServiceAgencySwitch(
  @EmbeddedId
  val id: ServiceAgencySwitchId,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ServiceAgencySwitch
    return id.externalService.serviceName == other.id.externalService.serviceName &&
      id.agencyLocation.id == other.id.agencyLocation.id
  }

  override fun hashCode(): Int = this.javaClass.hashCode()
}
