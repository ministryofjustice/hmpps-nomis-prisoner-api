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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@EntityOpen
@Table(name = "QUESTIONNAIRES")
data class Questionnaire(
  @Id
  @Column(name = "QUESTIONNAIRE_ID")
  @SequenceGenerator(name = "QUESTIONNAIRE_ID", sequenceName = "QUESTIONNAIRE_ID", allocationSize = 1)
  @GeneratedValue(generator = "QUESTIONNAIRE_ID")
  val id: Long = 0,

  @Column(name = "DESCRIPTION")
  val description: String? = null,

  @Column
  var code: String,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = true,

  @Column(name = "QUESTIONNAIRE_CATEGORY", nullable = false)
  val category: String = "IR_TYPE",

  @Column(name = "LIST_SEQ", nullable = false)
  val listSequence: Int,

  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "QUESTIONNAIRE_ID", nullable = false)
  val questions: MutableList<QuestionnaireQuestion> = mutableListOf(),

  @Column
  var auditModuleName: String? = null,

  @Column(name = "EXPIRY_DATE")
  val expiryDate: LocalDate? = null,
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
    other as Questionnaire

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id, code = $code, description = $description)"
  }
}
