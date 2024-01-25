package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.DiscriminatorFormula
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
class IncidentPartyId(
  @Column(name = "INCIDENT_CASE_ID", nullable = false)
  var incidentId: Long,

  @Column(name = "PARTY_SEQ", nullable = false)
  var partySequence: Int,
) : Serializable

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when staff_id is null then 'offender' else 'staff' end")
@Table(name = "INCIDENT_CASE_PARTIES")
open class IncidentParty(

  @EmbeddedId
  val id: IncidentPartyId,

  // Combination of :
  // Staff roles - Reference Codes code=IR_STF_PART
  // Offender roles - from questionnaireOffenderRole QUESTIONNAIRE_ROLES table
  @Column(name = "PARTICIPATION_ROLE")
  val role: String,

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
