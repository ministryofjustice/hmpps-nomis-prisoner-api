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
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
class IncidentPartyId(
  @Column(name = "INCIDENT_CASE_ID", nullable = false)
  var incidentCaseId: Long,

  @Column(name = "PARTY_SEQ", nullable = false)
  var partySequence: Int,
) : Serializable

@Entity
@Table(name = "INCIDENT_CASE_PARTIES")
class IncidentParty(

  @EmbeddedId
  val id: IncidentPartyId,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "INCIDENT_CASE_ID", insertable = false, updatable = false)
  val incident: Incident,

  // Combination of Reference Codes: Staff role code=IR_STF_PART and Offender role code = IR_OFF_PART
  @Column(name = "PARTICIPATION_ROLE")
  val role: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STAFF_ID")
  val staff: Staff? = null,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + Outcome.IR_OUTCOME + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "OUTCOME_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val outcome: Outcome? = null,
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as IncidentParty
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
fun IncidentParty.isStaffParty(): Boolean = staff != null
fun IncidentParty.isOffenderParty(): Boolean = offenderBooking != null

fun IncidentParty.staffParty(): Staff = staff!!
fun IncidentParty.offenderParty(): Offender = offenderBooking!!.offender
