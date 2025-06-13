package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@Embeddable
data class IncidentRequirementId(
  @Column(name = "INCIDENT_CASE_ID", nullable = false)
  val incidentId: Long,

  @Column(name = "REQUIREMENT_SEQ", nullable = false)
  val requirementSequence: Int,
)

@Entity
@Table(name = "INCIDENT_CASE_REQUIREMENTS")
@EntityOpen
class IncidentRequirement(

  @EmbeddedId
  val id: IncidentRequirementId,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  var agency: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RECORD_STAFF_ID", nullable = false)
  var recordingStaff: Staff,

  @Column(name = "RECORD_DATE", nullable = false)
  var recordedDate: LocalDateTime,
) : Comparable<IncidentRequirement> {

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
    private val COMPARATOR = compareBy<IncidentRequirement>
      { it.id.incidentId }
      .thenBy { it.id.requirementSequence }
  }

  override fun compareTo(other: IncidentRequirement) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as IncidentRequirement

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()
}
