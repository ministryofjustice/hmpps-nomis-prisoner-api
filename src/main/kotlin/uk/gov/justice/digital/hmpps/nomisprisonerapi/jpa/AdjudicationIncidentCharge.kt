package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
class AdjudicationIncidentChargeId(
  @Column(name = "AGENCY_INCIDENT_ID", nullable = false)
  var agencyIncidentId: Long,

  // sequence at Incident level
  @Column(name = "CHARGE_SEQ", nullable = false)
  var chargeSequence: Int,
) : Serializable

@Entity
@Table(name = "AGENCY_INCIDENT_CHARGES")
class AdjudicationIncidentCharge(

  @EmbeddedId
  val id: AdjudicationIncidentChargeId,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGENCY_INCIDENT_ID", insertable = false, updatable = false)
  val incident: AdjudicationIncident,

  @Column(name = "PARTY_SEQ", nullable = false)
  val partySequence: Int,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CHARGED_OIC_OFFENCE_ID")
  val offence: AdjudicationIncidentOffence,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "AGENCY_INCIDENT_ID",
        referencedColumnName = "AGENCY_INCIDENT_ID",
        insertable = false,
        updatable = false,
      ),
      JoinColumn(
        name = "PARTY_SEQ",
        referencedColumnName = "PARTY_SEQ",
        insertable = false,
        updatable = false,
      ),
    ],
  )
  val incidentParty: AdjudicationIncidentParty,

  @Column(name = "GUILTY_EVIDENCE_TEXT")
  val guiltyEvidence: String? = null,

  @Column(name = "REPORT_TEXT")
  val reportDetails: String? = null,

  // set by trigger year digits followed by max sequence for party eg 2301
  @Column(name = "LIDS_CHARGE_NUMBER", updatable = false, insertable = false)
  val lidsChargeNumber: Long? = 0,

  // adjudication number / charge index at incident level eg 4577667/1
  @Column(name = "OIC_CHARGE_ID")
  val offenceId: String? = null,

  @Column(name = "CREATE_DATETIME", nullable = false)
  var whenCreated: LocalDateTime = LocalDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationIncidentCharge
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
