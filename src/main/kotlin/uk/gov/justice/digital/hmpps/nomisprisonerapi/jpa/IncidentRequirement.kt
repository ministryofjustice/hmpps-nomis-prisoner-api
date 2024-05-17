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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
open class IncidentRequirementId(
  @Column(name = "INCIDENT_CASE_ID", nullable = false)
  var incidentId: Long,

  @Column(name = "REQUIREMENT_SEQ", nullable = false)
  var requirementSequence: Int,
) : Serializable

@Entity
@Table(name = "INCIDENT_CASE_REQUIREMENTS")
@EntityOpen
open class IncidentRequirement(

  @EmbeddedId
  val id: IncidentRequirementId,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val location: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RECORD_STAFF_ID", updatable = false, nullable = false)
  val recordingStaff: Staff,

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  @Generated
  var lastModifiedUsername: String? = null,

  @Column(name = "MODIFY_DATETIME", insertable = false, updatable = false)
  @Generated
  var lastModifiedDateTime: LocalDateTime? = null,

  // ---- NOT MAPPED Columns ---- //
  // All AUDIT data

) {
  @Column(name = "RECORD_DATE", insertable = false, updatable = false)
  @Generated
  lateinit var recordedDate: LocalDate

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as IncidentRequirement
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
