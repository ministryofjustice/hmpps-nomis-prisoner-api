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
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.util.Objects

@Entity
@Table(name = "AGENCY_INTERNAL_LOCATIONS")
@EntityOpen
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
  val certified: Boolean = false,

  @Column(name = "TRACKING_FLAG")
  @Convert(converter = YesNoConverter::class)
  val tracking: Boolean = false,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'${InternalLocationType.ILOC_TYPE}'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(name = "INTERNAL_LOCATION_TYPE", referencedColumnName = "code", nullable = false),
      ),
    ],
  )
  val locationType: InternalLocationType,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val agency: AgencyLocation,

  // calculated by trigger in Nomis
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

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "INTERNAL_LOCATION_CODE")
  val locationCode: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'${HousingUnitType.HOU_UN_TYPE}'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(name = "UNIT_TYPE", referencedColumnName = "code", nullable = false),
      ),
    ],
  )
  val unitType: HousingUnitType? = null,

  val capacity: Int? = null,

  @Column(name = "LIST_SEQ")
  val listSequence: Int? = null,

  @Column(name = "CNA_NO")
  val cnaCapacity: Int? = null,
) {

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
