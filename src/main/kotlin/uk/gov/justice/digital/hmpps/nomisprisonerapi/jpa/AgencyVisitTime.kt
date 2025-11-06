package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "AGENCY_VISIT_TIMES")
data class AgencyVisitTime(
  @EmbeddedId
  var agencyVisitTimesId: AgencyVisitTimeId,

  @Column(name = "START_TIME", nullable = false)
  val startTime: LocalTime,

  @Column(name = "END_TIME", nullable = false)
  val endTime: LocalTime,

  @Column(nullable = false)
  val effectiveDate: LocalDate,

  @Column
  val expiryDate: LocalDate?,

  @OneToMany(mappedBy = "agencyVisitTime", cascade = [ALL], orphanRemoval = true)
  val visitSlots: MutableList<AgencyVisitSlot> = mutableListOf(),

) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyVisitTime

    return agencyVisitTimesId == other.agencyVisitTimesId
  }

  override fun hashCode(): Int = agencyVisitTimesId.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $agencyVisitTimesId )"
}

@Embeddable
data class AgencyVisitTimeId(

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  var location: AgencyLocation,

  @Column(name = "WEEK_DAY", nullable = false)
  @Enumerated(EnumType.STRING)
  var weekDay: WeekDay,

  @Column(name = "TIME_SLOT_SEQ", nullable = false)
  var timeSlotSequence: Int,
) : Serializable
