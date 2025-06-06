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
import org.hibernate.type.YesNoConverter
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "INCIDENT_CASES")
@EntityOpen
class Incident(
  @Id
  @Column(name = "INCIDENT_CASE_ID")
  val id: Long = 0,

  @Column(name = "INCIDENT_TITLE")
  val title: String? = null,

  @Column(name = "INCIDENT_DETAILS")
  val description: String? = null,

  // Maps to the code of the questionnaire (so is really just the same mapping as questionnaire)
  @JoinColumn(nullable = false)
  val incidentType: String,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  var agency: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "QUESTIONNAIRE_ID", nullable = false)
  val questionnaire: Questionnaire,

  @OneToMany(mappedBy = "id.incidentId", cascade = [CascadeType.ALL], orphanRemoval = true)
  val questions: MutableList<IncidentQuestion> = mutableListOf(),

  @OneToMany(mappedBy = "id.incidentId", cascade = [CascadeType.ALL], orphanRemoval = true)
  var offenderParties: MutableList<IncidentOffenderParty> = mutableListOf(),

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "INCIDENT_CASE_ID", nullable = false)
  var incidentHistory: MutableList<IncidentHistory> = mutableListOf(),

  @OneToMany(mappedBy = "id.incidentId", cascade = [CascadeType.ALL], orphanRemoval = true)
  var staffParties: MutableList<IncidentStaffParty> = mutableListOf(),

  @OneToMany(mappedBy = "id.incidentId", cascade = [CascadeType.ALL], orphanRemoval = true)
  val requirements: MutableList<IncidentRequirement> = mutableListOf(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REPORTED_STAFF_ID", nullable = false)
  val reportingStaff: Staff,
  @Column(name = "REPORT_DATE", nullable = false)
  val reportedDate: LocalDate,
  @Column(name = "REPORT_TIME", nullable = false)
  val reportedTime: LocalTime,

  @Column(name = "FOLLOW_UP_DATE")
  val followUpDate: LocalDate? = null,

  @Column(name = "INCIDENT_DATE", nullable = false)
  val incidentDate: LocalDate,
  @Column(name = "INCIDENT_TIME", nullable = false)
  val incidentTime: LocalTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "INCIDENT_STATUS", nullable = false)
  val status: IncidentStatus,

  @Column(name = "RESPONSE_LOCKED_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val lockedResponse: Boolean = false,

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  @Generated
  var lastModifiedUsername: String? = null,

  @Column(name = "MODIFY_DATETIME", insertable = false, updatable = false)
  @Generated
  var lastModifiedDateTime: LocalDateTime? = null,

  // ---- NOT MAPPED columns ---- //
  // All AUDIT data
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @CreatedBy
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @CreatedDate
  lateinit var createDatetime: LocalDateTime

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
