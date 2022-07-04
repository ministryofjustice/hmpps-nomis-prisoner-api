package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "AGENCY_VISIT_DAYS")
data class AgencyVisitDay(
  @EmbeddedId
  val agencyVisitDayId: AgencyVisitDayId
)

@Embeddable
data class AgencyVisitDayId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation,

  @Column(name = "WEEK_DAY", nullable = false)
  val weekDay: String
) : Serializable
