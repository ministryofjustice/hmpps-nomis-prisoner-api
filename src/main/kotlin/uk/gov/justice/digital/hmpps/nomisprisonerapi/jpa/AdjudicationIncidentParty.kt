package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

const val suspectRole = "S"
const val witnessRole = "W"
const val victimRole = "V"
const val reportingOfficerRole = "RO"
const val forceControllingOfficerRole = "CR"
const val otherRole = "OTH"

@Embeddable
class AdjudicationIncidentPartyId(
  @Column(name = "AGENCY_INCIDENT_ID", nullable = false)
  var agencyIncidentId: Long,

  @Column(name = "PARTY_SEQ", nullable = false)
  var partySequence: Int,
) : Serializable

@Entity
@Table(name = "AGENCY_INCIDENT_PARTIES")
class AdjudicationIncidentParty(

  @EmbeddedId
  val id: AdjudicationIncidentPartyId,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking? = null,

  // @MapsId("agencyIncidentId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGENCY_INCIDENT_ID", insertable = false, updatable = false)
  val incident: AdjudicationIncident,

  @Column
  val incidentRole: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STAFF_ID")
  val staff: Staff? = null,

  @Column
  val partyAddedDate: LocalDate = LocalDate.now(),

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IncidentDecisionAction.INC_DECISION + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "ACTION_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val actionDecision: IncidentDecisionAction? = null,

  @OneToMany(mappedBy = "incidentParty", cascade = [CascadeType.ALL], orphanRemoval = true)
  val charges: MutableList<AdjudicationIncidentCharge> = mutableListOf(),

  @OneToMany(mappedBy = "incidentParty", cascade = [CascadeType.ALL], orphanRemoval = true)
  val investigations: MutableList<AdjudicationInvestigation> = mutableListOf(),

  @Column(unique = true, name = "OIC_INCIDENT_ID")
  val adjudicationNumber: Long? = null,

  @Column(name = "CREATE_DATETIME", nullable = false)
  var whenCreated: LocalDateTime = LocalDateTime.now(),

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationIncidentParty
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

fun AdjudicationIncidentParty.prisonerOnReport(): OffenderBooking = offenderBooking.takeIf { this.adjudicationNumber != null }!!
fun AdjudicationIncidentParty.staffParty(): Staff = staff!!
fun AdjudicationIncidentParty.isSuspect(): Boolean = incidentRole == suspectRole
fun AdjudicationIncidentParty.isWitness(): Boolean = incidentRole == witnessRole
fun AdjudicationIncidentParty.isVictim(): Boolean = incidentRole == victimRole
fun AdjudicationIncidentParty.isReportingOfficer(): Boolean = incidentRole == reportingOfficerRole
fun AdjudicationIncidentParty.isInvolvedForForce(): Boolean = incidentRole == forceControllingOfficerRole
fun AdjudicationIncidentParty.isInvolvedForOtherReason(): Boolean = incidentRole == otherRole
fun AdjudicationIncidentParty.prisonerParty(): Offender = offenderBooking!!.offender

fun List<AdjudicationIncidentParty>.findAdjudication(adjudicationNumber: Long): AdjudicationIncidentParty =
  this.find { it.adjudicationNumber == adjudicationNumber }!!
