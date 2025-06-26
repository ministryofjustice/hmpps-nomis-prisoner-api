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
data class IncidentResponseId(
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns(
    JoinColumn(name = "INCIDENT_CASE_ID", referencedColumnName = "INCIDENT_CASE_ID", nullable = false),
    JoinColumn(name = "QUESTION_SEQ", referencedColumnName = "QUESTION_SEQ", nullable = false),
  )
  val incidentQuestion: IncidentQuestion,

  @Column(name = "RESPONSE_SEQ", nullable = false)
  val responseSequence: Int,
) : Serializable

@Entity
@Table(name = "INCIDENT_CASE_RESPONSES")
@EntityOpen
class IncidentResponse(

  @EmbeddedId
  val id: IncidentResponseId,

  @ManyToOne
  @JoinColumn(name = "QUESTIONNAIRE_ANS_ID")
  var answer: QuestionnaireAnswer?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RECORD_STAFF_ID", nullable = false)
  var recordingStaff: Staff,

  @Column(name = "RESPONSE_DATE")
  var responseDate: LocalDate? = null,

  @Column(name = "RESPONSE_COMMENT_TEXT")
  var comment: String? = null,
) : Comparable<IncidentResponse> {
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
    private val COMPARATOR = compareBy<IncidentResponse>
      { it.id.incidentQuestion }
      .thenBy { it.id.responseSequence }
  }

  override fun compareTo(other: IncidentResponse) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as IncidentResponse

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id ), ${answer?.answerText})"
}
