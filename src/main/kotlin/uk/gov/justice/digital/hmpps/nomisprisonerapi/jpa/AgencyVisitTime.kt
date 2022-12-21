package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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

  @Column(nullable = false)
  val expiryDate: LocalDate,

)

@Embeddable
data class AgencyVisitTimeId(

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  var location: AgencyLocation,

  @Column(name = "WEEK_DAY", nullable = false)
  var weekDay: String,

  @Column(name = "TIME_SLOT_SEQ", nullable = false)
  var timeSlotSequence: Int,
) : Serializable
