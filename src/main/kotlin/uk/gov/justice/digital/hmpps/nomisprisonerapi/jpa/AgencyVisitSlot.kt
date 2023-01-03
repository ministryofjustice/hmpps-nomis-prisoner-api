package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.util.Objects

@Entity
@Table(name = "AGENCY_VISIT_SLOTS")
data class AgencyVisitSlot(
  @Id
  @SequenceGenerator(name = "AGENCY_VISIT_SLOT_ID", sequenceName = "AGENCY_VISIT_SLOT_ID", allocationSize = 1)
  @GeneratedValue(generator = "AGENCY_VISIT_SLOT_ID")
  @Column(name = "AGENCY_VISIT_SLOT_ID")
  var id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  var location: AgencyLocation,

  @Column(name = "WEEK_DAY", nullable = false)
  var weekDay: String,

  @Column(name = "TIME_SLOT_SEQ", nullable = false)
  var timeSlotSequence: Int,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(name = "WEEK_DAY", referencedColumnName = "WEEK_DAY", insertable = false, updatable = false),
      JoinColumn(name = "AGY_LOC_ID", referencedColumnName = "AGY_LOC_ID", insertable = false, updatable = false),
      JoinColumn(name = "TIME_SLOT_SEQ", referencedColumnName = "TIME_SLOT_SEQ", insertable = false, updatable = false)
    ]
  )
  var agencyVisitTime: AgencyVisitTime,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "INTERNAL_LOCATION_ID", nullable = false)
  var agencyInternalLocation: AgencyInternalLocation,

  @Column
  var maxGroups: Int? = null,

  @Column
  var maxAdults: Int? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyVisitSlot
    return id == other.id
  }

  override fun hashCode(): Int {
    return Objects.hashCode(id)
  }
}
