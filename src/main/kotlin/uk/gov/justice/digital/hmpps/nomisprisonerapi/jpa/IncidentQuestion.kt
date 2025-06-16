package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@Embeddable
data class IncidentQuestionId(
  @Column(name = "INCIDENT_CASE_ID", nullable = false)
  val incidentId: Long,

  @Column(name = "QUESTION_SEQ", nullable = false)
  val questionSequence: Int,
)

@Entity
@Table(name = "INCIDENT_CASE_QUESTIONS")
@EntityOpen
class IncidentQuestion(

  @EmbeddedId
  val id: IncidentQuestionId,

  @ManyToOne
  @JoinColumn(name = "QUESTIONNAIRE_QUE_ID")
  var question: QuestionnaireQuestion,

  @OneToMany(mappedBy = "id.incidentQuestion", cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val responses: MutableList<IncidentResponse> = mutableListOf(),
) : Comparable<IncidentQuestion> {

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  companion object {
    private val COMPARATOR = compareBy<IncidentQuestion>
      { it.id.incidentId }
      .thenBy { it.id.questionSequence }
  }

  override fun compareTo(other: IncidentQuestion) = COMPARATOR.compare(this, other)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as IncidentQuestion

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id ), question = ${question.questionText})"
}
