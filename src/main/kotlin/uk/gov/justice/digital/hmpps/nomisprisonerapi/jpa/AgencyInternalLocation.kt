package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.util.Objects

@Entity
@Table(name = "AGENCY_INTERNAL_LOCATIONS")
data class AgencyInternalLocation(
  @Id
  @SequenceGenerator(name = "INTERNAL_LOCATION_ID", sequenceName = "INTERNAL_LOCATION_ID", allocationSize = 1)
  @GeneratedValue(generator = "INTERNAL_LOCATION_ID")
  @Column(name = "INTERNAL_LOCATION_ID")
  val locationId: Long = 0,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,

  @Column(name = "CERTIFIED_FLAG")
  @Convert(converter = YesNoConverter::class)
  val certifiedFlag: Boolean = false,

  @Column(name = "INTERNAL_LOCATION_TYPE")
  val locationType: String,

  @Column(name = "AGY_LOC_ID")
  val agencyId: String,

  @Column(name = "DESCRIPTION")
  val description: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_INTERNAL_LOCATION_ID")
  val parentLocation: AgencyInternalLocation? = null,

  @Column(name = "NO_OF_OCCUPANT")
  var currentOccupancy: Int? = null,

  @Column(name = "OPERATION_CAPACITY")
  val operationalCapacity: Int? = null,

  @Column(name = "USER_DESC")
  val userDescription: String? = null,

  @Column(name = "INTERNAL_LOCATION_CODE")
  val locationCode: String,

  @Column(name = "CAPACITY")
  val capacity: Int? = null
) {
  val isCell: Boolean
    get() = locationType == "CELL"
  val isCellSwap: Boolean
    get() = !certifiedFlag &&
      active && parentLocation == null && locationCode == "CSWAP"
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
