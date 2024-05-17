package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "INCIDENT_QUESTIONNAIRE_HTY")
@EntityOpen
data class IncidentHistory(

  @Id
  @Column(name = "INCIDENT_QUESTIONNAIRE_ID")
  @SequenceGenerator(name = "INCIDENT_QUESTIONNAIRE_ID", sequenceName = "INCIDENT_QUESTIONNAIRE_ID", allocationSize = 1)
  @GeneratedValue(generator = "INCIDENT_QUESTIONNAIRE_ID")
  val id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "QUESTIONNAIRE_ID", nullable = false)
  val questionnaire: Questionnaire,

  @Column(name = "CHANGE_DATE")
  val incidentChangeDate: LocalDate = LocalDate.now(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CHANGE_STAFF_ID")
  val incidentChangeStaff: Staff,

  @OneToMany(mappedBy = "id.incidentHistory", cascade = [CascadeType.ALL], orphanRemoval = true)
  val questions: MutableList<IncidentQuestionHistory> = mutableListOf(),

  // ---- NOT MAPPED columns ---- //
  // MODIFY_USER_ID - not required
  // MODIFY_DATETIME - not required
  // All AUDIT data
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
    other as IncidentHistory

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id)"
  }
}
