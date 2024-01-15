package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.type.YesNoConverter
import org.springframework.data.annotation.LastModifiedBy
import java.time.LocalDateTime

@Entity
@Table(name = "QUESTIONNAIRE_QUESTIONS")
data class QuestionnaireQuestion(
  @Id
  @Column(name = "QUESTIONNAIRE_QUE_ID")
  @SequenceGenerator(name = "QUESTIONNAIRE_QUE_ID", sequenceName = "QUESTIONNAIRE_QUE_ID", allocationSize = 1)
  @GeneratedValue(generator = "QUESTIONNAIRE_QUE_ID")
  var id: Long = 0,

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "QUESTIONNAIRE_QUE_ID", nullable = false)
  val answers: MutableList<QuestionnaireAnswer> = mutableListOf(),

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = true,

  @Column(name = "QUE_SEQ")
  val questionSequence: Int,

  @Column(name = "DESCRIPTION")
  val question: String,

  @Column(name = "MULTIPLE_ANSWER_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val multipleAnswers: Boolean = true,

  @Column(name = "LIST_SEQ")
  val listSequence: Int,

  @Column
  var auditModuleName: String? = null,

  @Column(name = "MODIFY_DATETIME", nullable = false, insertable = false, updatable = false)
  var whenUpdated: LocalDateTime? = null,
  @Column(name = "MODIFY_USER_ID", updatable = false, insertable = false)
  @LastModifiedBy
  val modifiedBy: String? = null,
) {

  @Column(name = "CREATE_USER_ID", nullable = false, insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", nullable = false, insertable = false, updatable = false)
  @Generated
  lateinit var whenCreated: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as QuestionnaireQuestion

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id ), $question)"
  }
}
