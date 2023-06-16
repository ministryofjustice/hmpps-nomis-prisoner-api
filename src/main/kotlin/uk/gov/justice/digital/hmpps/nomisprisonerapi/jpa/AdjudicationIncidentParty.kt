package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.time.LocalDate

const val suspectRole = "S"

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

  @Column(name = "OIC_INCIDENT_ID")
  val adjudicationNumber: Long? = null,

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
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationIncidentParty
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

fun AdjudicationIncidentParty.suspect(): OffenderBooking = this.offenderBooking.takeIf { this.incidentRole == suspectRole }!!
