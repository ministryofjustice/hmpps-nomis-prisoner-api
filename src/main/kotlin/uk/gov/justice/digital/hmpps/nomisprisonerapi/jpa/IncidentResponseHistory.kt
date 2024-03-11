package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
class IncidentResponseHistoryId(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "INCIDENT_QUESTIONNAIRE_ID",
        referencedColumnName = "INCIDENT_QUESTIONNAIRE_ID",
        nullable = false,
      ),
      JoinColumn(
        name = "QUESTION_SEQ",
        referencedColumnName = "QUESTION_SEQ",
        nullable = false,
      ),
    ],
  )
  val incidentQuestion: IncidentQuestionHistory,

  @Column(name = "RESPONSE_SEQ", nullable = false)
  val responseSequence: Int,

) : Serializable

@Entity
@Table(name = "INCIDENT_QUE_RESPONSE_HTY")
@EntityOpen
data class IncidentResponseHistory(

  @EmbeddedId
  val id: IncidentResponseHistoryId,

  @ManyToOne
  @JoinColumn(name = "QUESTIONNAIRE_ANS_ID", updatable = false)
  val answer: QuestionnaireAnswer?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RECORD_STAFF_ID", updatable = false, nullable = false)
  val recordingStaff: Staff,

  @Column(name = "RESPONSE_DATE")
  val responseDate: LocalDate? = null,

  @Column(name = "RESPONSE_COMMENT_TEXT")
  val comment: String? = null,

  @Column
  var auditModuleName: String? = null,

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
    other as IncidentResponseHistory

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id)"
  }
}
