package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime
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
  var active: Boolean = false,

  @Column(name = "CERTIFIED_FLAG")
  @Convert(converter = YesNoConverter::class)
  var certified: Boolean = false,

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
  var locationType: InternalLocationType,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val agency: AgencyLocation,

  // calculated by trigger in Nomis
  @Column(name = "DESCRIPTION")
  var description: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_INTERNAL_LOCATION_ID")
  var parentLocation: AgencyInternalLocation? = null,

  @Column(name = "NO_OF_OCCUPANT")
  var currentOccupancy: Int? = null,

  @Column(name = "OPERATION_CAPACITY")
  var operationalCapacity: Int? = null,

  @Column(name = "USER_DESC")
  var userDescription: String? = null,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,

  @Column(name = "INTERNAL_LOCATION_CODE")
  var locationCode: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'${HousingUnitType.HOU_UN_TYPE}'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(name = "UNIT_TYPE", referencedColumnName = "code", nullable = true),
      ),
    ],
  )
  var unitType: HousingUnitType? = null,

  var capacity: Int? = null,

  @Column(name = "LIST_SEQ")
  var listSequence: Int? = null,

  @Column(name = "CNA_NO")
  var cnaCapacity: Int? = null,

  var deactivateDate: LocalDate? = null,
  var reactivateDate: LocalDate? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'${LivingUnitReason.LIV_UN_RSN}'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(name = "DEACTIVATE_REASON_CODE", referencedColumnName = "code", nullable = true),
      ),
    ],
  )
  var deactivateReason: LivingUnitReason? = null,

  @OneToMany(mappedBy = "agencyInternalLocation", cascade = [CascadeType.ALL], orphanRemoval = true)
  val profiles: MutableList<AgencyInternalLocationProfile> = mutableListOf(),

  @OneToMany(mappedBy = "agencyInternalLocation", cascade = [CascadeType.ALL], orphanRemoval = true)
  val usages: MutableList<InternalLocationUsageLocation> = mutableListOf(),

) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  @Generated
  val modifyUsername: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyInternalLocation
    return locationId == other.locationId
  }

  override fun hashCode(): Int = Objects.hashCode(locationId)
}
