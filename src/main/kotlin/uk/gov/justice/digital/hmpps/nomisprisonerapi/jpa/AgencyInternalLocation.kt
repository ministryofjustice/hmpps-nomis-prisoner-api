package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import net.bytebuddy.build.ToStringPlugin
import org.hibernate.Hibernate
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Type
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "AGENCY_INTERNAL_LOCATIONS")
@BatchSize(size = 25)
data class AgencyInternalLocation(
  @Id
  @Column(name = "INTERNAL_LOCATION_ID")
  val locationId: Long = 0,

  @Column(name = "ACTIVE_FLAG")
  @Type(type = "yes_no")
  val active: Boolean = false,

  @Column(name = "CERTIFIED_FLAG")
  @Type(type = "yes_no")
  val certifiedFlag: Boolean = false,

  @Column(name = "INTERNAL_LOCATION_TYPE")
  val locationType: String? = null,

  @Column(name = "AGY_LOC_ID")
  val agencyId: String? = null,

  @Column(name = "DESCRIPTION")
  val description: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_INTERNAL_LOCATION_ID")
  @ToStringPlugin.Exclude
  val parentLocation: AgencyInternalLocation? = null,

  @Column(name = "NO_OF_OCCUPANT")
  var currentOccupancy: Int? = null,

  @Column(name = "OPERATION_CAPACITY")
  val operationalCapacity: Int? = null,

  @Column(name = "USER_DESC")
  val userDescription: String? = null,

  @Column(name = "INTERNAL_LOCATION_CODE")
  val locationCode: String? = null,

  @Column(name = "CAPACITY")
  val capacity: Int? = null
) {
  val isCell: Boolean
    get() = locationType != null && locationType == "CELL"
  val isCellSwap: Boolean
    get() = !certifiedFlag &&
      active && parentLocation == null && locationCode != null && locationCode == "CSWAP"
  val isActiveCell: Boolean
    get() = active && isCell

  fun hasSpace(treatZeroOperationalCapacityAsNull: Boolean): Boolean {
    val capacity = getActualCapacity(treatZeroOperationalCapacityAsNull)
    return capacity != null && currentOccupancy != null && currentOccupancy!! < capacity
  }

  fun decrementCurrentOccupancy(): Int {
    currentOccupancy = if (currentOccupancy != null && currentOccupancy!! > 0) {
      currentOccupancy!! - 1
    } else {
      0
    }
    return currentOccupancy!!
  }

  fun isActiveCellWithSpace(treatZeroOperationalCapacityAsNull: Boolean): Boolean {
    return isActiveCell && hasSpace(treatZeroOperationalCapacityAsNull)
  }

  fun getActualCapacity(treatZeroOperationalCapacityAsNull: Boolean): Int? {
    val useOperationalCapacity =
      operationalCapacity != null && !(treatZeroOperationalCapacityAsNull && operationalCapacity == 0)
    return if (useOperationalCapacity) operationalCapacity else capacity
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyInternalLocation
    return locationId == other.locationId
  }

  override fun hashCode(): Int {
    return Objects.hashCode(locationId)
  }
}
