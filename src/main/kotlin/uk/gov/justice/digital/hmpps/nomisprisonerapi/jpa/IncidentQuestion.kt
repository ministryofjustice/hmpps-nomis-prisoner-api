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
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
class IncidentQuestionId(
  @Column(name = "INCIDENT_CASE_ID", nullable = false)
  var incidentId: Long,

  @Column(name = "QUESTION_SEQ", nullable = false)
  var questionSequence: Int,
) : Serializable

@Entity
@Table(name = "INCIDENT_CASE_QUESTIONS")
data class IncidentQuestion(

  @EmbeddedId
  val id: IncidentQuestionId,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "QUESTIONNAIRE_QUE_ID", updatable = false, nullable = false)
  val question: QuestionnaireQuestion,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "INCIDENT_CASE_ID", insertable = false, updatable = false, nullable = false)
  val incident: Incident,

/*
  @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "INCIDENT_CASE_ID",
        referencedColumnName = "INCIDENT_CASE_ID",
        nullable = false,
      ),
      JoinColumn(
        name = "QUESTION_SEQ",
        referencedColumnName = "QUESTION_SEQ",
        nullable = false,
      ),
    ],
  )
  val responses: MutableList<IncidentResponse> = mutableListOf(),
 */

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
    other as IncidentQuestion

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id ), ${question.question})"
  }
}
