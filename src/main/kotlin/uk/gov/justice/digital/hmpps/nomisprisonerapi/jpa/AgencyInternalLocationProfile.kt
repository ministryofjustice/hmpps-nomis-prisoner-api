package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable

@Embeddable
class AgencyInternalLocationProfileId(
  @Column(name = "INTERNAL_LOCATION_ID", nullable = false)
  var locationId: Long,

  /**
   * Reference Domain for the attribute
   */
  @Column(name = "INT_LOC_PROFILE_TYPE", nullable = false)
  val profileType: String,

  /**
   * Reference Code in the domain above for the attribute
   */
  @Column(name = "INT_LOC_PROFILE_CODE", nullable = false)
  val profileCode: String,
) : Serializable

@Entity
@Table(name = "AGY_INT_LOC_PROFILES")
@EntityOpen
data class AgencyInternalLocationProfile(

  @EmbeddedId
  val id: AgencyInternalLocationProfileId,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "INTERNAL_LOCATION_ID", insertable = false, updatable = false)
  val agencyInternalLocation: AgencyInternalLocation,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyInternalLocationProfile
    return id.locationId == other.id.locationId &&
      id.profileType == other.id.profileType &&
      id.profileCode == other.id.profileCode
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String =
    "AgencyInternalLocationProfile(locationId=${id.locationId}, profileType=${id.profileType}, profileCode=${id.profileCode})"
}
