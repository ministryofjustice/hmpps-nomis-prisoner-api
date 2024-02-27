package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.util.Objects

@Entity
@Table(name = "INT_LOC_USAGE_LOCATIONS")
@EntityOpen
data class InternalLocationUsageLocation(
  @Id
  @SequenceGenerator(name = "USAGE_LOCATION_ID", sequenceName = "USAGE_LOCATION_ID", allocationSize = 1)
  @GeneratedValue(generator = "USAGE_LOCATION_ID")
  val usageLocationId: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "INTERNAL_LOCATION_USAGE_ID")
  val internalLocationUsage: InternalLocationUsage,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "INTERNAL_LOCATION_ID")
  val agencyInternalLocation: AgencyInternalLocation,

  var capacity: Int? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(value = "'${InternalLocationType.ILOC_TYPE}'", referencedColumnName = "domain"),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(name = "USAGE_LOCATION_TYPE", referencedColumnName = "code", nullable = true),
      ),
    ],
  )
  val usageLocationType: InternalLocationType? = null,

  @Column(name = "LIST_SEQ")
  var listSequence: Int? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_USAGE_LOCATION_ID")
  val parentUsage: InternalLocationUsageLocation? = null,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as InternalLocationUsageLocation
    return usageLocationId == other.usageLocationId
  }

  override fun hashCode(): Int = Objects.hashCode(usageLocationId)

  override fun toString(): String =
    "InternalLocationUsageLocation(usageLocationId=$usageLocationId, internalLocationUsage=$internalLocationUsage, capacity=$capacity, usageLocationType=$usageLocationType, listSequence=$listSequence)"
}
