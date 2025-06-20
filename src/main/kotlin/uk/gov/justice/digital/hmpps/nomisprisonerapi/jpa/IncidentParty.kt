package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import org.hibernate.annotations.DiscriminatorFormula
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@Embeddable
data class IncidentPartyId(
  @Column(name = "INCIDENT_CASE_ID", nullable = false)
  val incidentId: Long,

  @Column(name = "PARTY_SEQ", nullable = false)
  val partySequence: Int,
)

@Entity
@EntityOpen
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when staff_id is null then 'offender' else 'staff' end")
@Table(name = "INCIDENT_CASE_PARTIES")
class IncidentParty(

  @EmbeddedId
  val id: IncidentPartyId,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,
) : Comparable<IncidentParty> {

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  @Generated
  var lastModifiedUsername: String? = null

  @Column(name = "MODIFY_DATETIME", insertable = false, updatable = false)
  @Generated
  var lastModifiedDateTime: LocalDateTime? = null

  companion object {
    private val COMPARATOR = compareBy<IncidentParty>
      { it.id.incidentId }
      .thenBy { it.id.partySequence }
  }

  override fun compareTo(other: IncidentParty) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as IncidentParty

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
