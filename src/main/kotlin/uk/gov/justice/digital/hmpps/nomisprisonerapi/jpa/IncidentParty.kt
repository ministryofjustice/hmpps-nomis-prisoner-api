package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.DiscriminatorFormula
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
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
@EntityOpen
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when staff_id is null then 'offender' else 'staff' end")
@Table(name = "INCIDENT_CASE_PARTIES")
class IncidentParty(

  @EmbeddedId
  val id: IncidentPartyId,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

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
