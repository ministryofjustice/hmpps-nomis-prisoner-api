package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.SortNatural
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SortedSet

@Entity
@Table(name = "INCIDENT_CASES")
@EntityOpen
class Incident(
  @Id
  @Column(name = "INCIDENT_CASE_ID")
  val id: Long = 0,

  @Column(name = "INCIDENT_TITLE")
  var title: String? = null,

  @Column(name = "INCIDENT_DETAILS")
  var description: String? = null,

  // Maps to the code of the questionnaire (so is really just the same mapping as questionnaire)
  @JoinColumn(nullable = false)
  var incidentType: String,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  var agency: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "QUESTIONNAIRE_ID", nullable = false)
  var questionnaire: Questionnaire,

  @OneToMany(mappedBy = "id.incidentId", cascade = [CascadeType.ALL], orphanRemoval = true)
  val questions: MutableList<IncidentQuestion> = mutableListOf(),

  @OneToMany(mappedBy = "id.incidentId", cascade = [CascadeType.ALL], orphanRemoval = true)
  val offenderParties: MutableList<IncidentOffenderParty> = mutableListOf(),

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "INCIDENT_CASE_ID", nullable = false)
  val incidentHistory: MutableList<IncidentHistory> = mutableListOf(),

  @OneToMany(mappedBy = "id.incidentId", cascade = [CascadeType.ALL], orphanRemoval = true)
  val staffParties: MutableList<IncidentStaffParty> = mutableListOf(),

  @OneToMany(mappedBy = "id.incidentId", cascade = [CascadeType.ALL], orphanRemoval = true)
  @SortNatural
  val requirements: SortedSet<IncidentRequirement> = sortedSetOf(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REPORTED_STAFF_ID", nullable = false)
  var reportingStaff: Staff,
  @Column(name = "REPORT_DATE", nullable = false)
  var reportedDate: LocalDateTime,
  @Column(name = "REPORT_TIME", nullable = false)
  var reportedTime: LocalDateTime,

  @Column(name = "FOLLOW_UP_DATE")
  val followUpDate: LocalDate? = null,

  @Column(name = "INCIDENT_DATE", nullable = false)
  var incidentDate: LocalDateTime,
  @Column(name = "INCIDENT_TIME", nullable = false)
  var incidentTime: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "INCIDENT_STATUS", nullable = false)
  var status: IncidentStatus,

  @Column(name = "RESPONSE_LOCKED_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val lockedResponse: Boolean = false,
) {
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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Incident

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id, description = $description)"
}
