package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
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
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "AGENCY_INCIDENTS")
class AdjudicationIncident(

  @Id
  @SequenceGenerator(name = "AGENCY_INCIDENT_ID", sequenceName = "AGENCY_INCIDENT_ID", allocationSize = 1)
  @GeneratedValue(generator = "AGENCY_INCIDENT_ID")
  @Column(name = "AGENCY_INCIDENT_ID")
  val id: Long = 0,

  @OneToMany(mappedBy = "incident", cascade = [CascadeType.ALL], orphanRemoval = true)
  val parties: MutableList<AdjudicationIncidentParty> = mutableListOf(),

  @OneToMany(mappedBy = "incident", cascade = [CascadeType.ALL], orphanRemoval = true)
  val repairs: MutableList<AdjudicationIncidentRepair> = mutableListOf(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REPORTED_STAFF_ID")
  val reportingStaff: Staff,

  @Column
  val incidentDate: LocalDate = LocalDate.now(),

  @Column(name = "INCIDENT_TIME")
  val incidentDateTime: LocalDateTime = LocalDateTime.now(),

  @Column(name = "REPORT_DATE")
  val reportedDate: LocalDate = LocalDate.now(),

  @Column(name = "REPORT_TIME")
  val reportedDateTime: LocalDateTime = LocalDateTime.now(),

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "INTERNAL_LOCATION_ID", nullable = false)
  val agencyInternalLocation: AgencyInternalLocation,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationIncidentType.INC_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "INCIDENT_TYPE", referencedColumnName = "code")),
    ],
  )
  val incidentType: AdjudicationIncidentType,

  val incidentStatus: String = "ACTIVE",

  @Column(name = "INCIDENT_DETAILS")
  val incidentDetails: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val prison: AgencyLocation,

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationIncident
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

fun AdjudicationIncident.findAdjudication(adjudicationNumber: Long): AdjudicationIncidentParty =
  this.parties.findAdjudication(adjudicationNumber)
