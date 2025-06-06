package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

const val SUSPECT_ROLE = "S"
const val WITNESS_ROLE = "W"
const val VICTIM_ROLE = "V"
const val REPORTING_OFFICER_ROLE = "RO"
const val FORCE_CONTROLLING_OFFICER_ROLE = "CR"
const val OTHER_ROLE = "OTH"

@Embeddable
class AdjudicationIncidentPartyId(
  @Column(name = "AGENCY_INCIDENT_ID", nullable = false)
  var agencyIncidentId: Long,

  @Column(name = "PARTY_SEQ", nullable = false)
  var partySequence: Int,
) : Serializable

@Entity
@Table(name = "AGENCY_INCIDENT_PARTIES")
@NamedEntityGraph(
  name = "full-adjudication",
  attributeNodes = [
    NamedAttributeNode(value = "actionDecision"),
    NamedAttributeNode(value = "incident", subgraph = "full-incident"),
    NamedAttributeNode(value = "offenderBooking", subgraph = "booking-offender"),
  ],
  subgraphs = [
    NamedSubgraph(
      name = "booking-offender",
      attributeNodes = [
        NamedAttributeNode("offender", subgraph = "offender-with-gender"),
        NamedAttributeNode("location"),
        NamedAttributeNode("visitBalance"),
      ],
    ),
    NamedSubgraph(
      name = "full-incident",
      attributeNodes = [
        NamedAttributeNode("agencyInternalLocation"),
        NamedAttributeNode("incidentType"),
        NamedAttributeNode("prison"),
        NamedAttributeNode("reportingStaff", subgraph = "staff-accounts"),

      ],
    ),
    NamedSubgraph(
      name = "offender-with-gender",
      attributeNodes = [
        NamedAttributeNode("gender"),
      ],
    ),
    NamedSubgraph(name = "staff-accounts", attributeNodes = [NamedAttributeNode("accounts")]),
  ],
)
class AdjudicationIncidentParty(

  @EmbeddedId
  val id: AdjudicationIncidentPartyId,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking? = null,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
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
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

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
fun AdjudicationIncidentParty.isSuspect(): Boolean = incidentRole == SUSPECT_ROLE
fun AdjudicationIncidentParty.isWitness(): Boolean = incidentRole == WITNESS_ROLE
fun AdjudicationIncidentParty.isVictim(): Boolean = incidentRole == VICTIM_ROLE
fun AdjudicationIncidentParty.isReportingOfficer(): Boolean = incidentRole == REPORTING_OFFICER_ROLE
fun AdjudicationIncidentParty.isInvolvedForForce(): Boolean = incidentRole == FORCE_CONTROLLING_OFFICER_ROLE
fun AdjudicationIncidentParty.isInvolvedForOtherReason(): Boolean = incidentRole == OTHER_ROLE
fun AdjudicationIncidentParty.prisonerParty(): Offender = offenderBooking!!.offender

fun List<AdjudicationIncidentParty>.findAdjudication(adjudicationNumber: Long): AdjudicationIncidentParty = this.find { it.adjudicationNumber == adjudicationNumber }!!
