package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.type.YesNoConverter
import org.springframework.data.annotation.CreatedDate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_CASE_NOTES")
@EntityOpen
@NamedEntityGraph(
  name = "offender-case-note",
  attributeNodes = [
    NamedAttributeNode(value = "offenderBooking"),
    NamedAttributeNode(value = "author", subgraph = "staff-accounts"),
    NamedAttributeNode(value = "caseNoteType"),
    NamedAttributeNode(value = "caseNoteSubType"),
  ],
  subgraphs = [
    NamedSubgraph(name = "staff-accounts", attributeNodes = [NamedAttributeNode("accounts")]),
  ],
)
class OffenderCaseNote(
  @Id
  @Column(name = "CASE_NOTE_ID", nullable = false)
  @SequenceGenerator(name = "CASE_NOTE_ID", sequenceName = "CASE_NOTE_ID", allocationSize = 1)
  @GeneratedValue(generator = "CASE_NOTE_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "CONTACT_DATE")
  val occurrenceDate: LocalDate,
  // Actually nullable but never null in prod data

  @Column(name = "CONTACT_TIME")
  var occurrenceDateTime: LocalDateTime,
  // Actually nullable but never null in prod data
  // date part always the same as CONTACT_DATE

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + TaskType.TASK_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "CASE_NOTE_TYPE", referencedColumnName = "code")),
    ],
  )
  var caseNoteType: TaskType,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + TaskSubType.TASK_SUBTYPE + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "CASE_NOTE_SUB_TYPE", referencedColumnName = "code")),
    ],
  )
  var caseNoteSubType: TaskSubType,

  @ManyToOne(optional = false)
  @JoinColumn(name = "STAFF_ID")
  val author: Staff,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val agencyLocation: AgencyLocation? = null,
  // can be null pre c.2017

  @Column(name = "CASE_NOTE_TEXT", nullable = false)
  var caseNoteText: String,
  // Actually nullable but never null in prod data

  @Column(name = "AMENDMENT_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val amendmentFlag: Boolean = false,

  // IWP_FLAG - not used
  // CHECK BOX 1 to 5 - not used
  // EVENT_ID - not used

  @Column(name = "NOTE_SOURCE_CODE", nullable = false)
  @Enumerated(EnumType.STRING)
  var noteSourceCode: NoteSourceCode,
  // Actually nullable but never null in prod data

  @Column(name = "DATE_CREATION", nullable = false)
  @CreatedDate
  var dateCreation: LocalDateTime,
  // Actually nullable but never null in prod data
  // Can be different to both the contact date and the create_datetime

  @Column(name = "TIME_CREATION")
  @CreatedDate
  var timeCreation: LocalDateTime? = null,
  // date part always the same as DATE_CREATION but CAN BE NULL in prod data

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  var createdDatetime: LocalDateTime = LocalDateTime.now(),

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  var createdUserId: String = "",

  @Column(name = "MODIFY_DATETIME", insertable = false, updatable = false)
  @Generated
  var modifiedDatetime: LocalDateTime? = null,

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  @Generated
  var modifiedUserId: String? = null,

  val auditModuleName: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderCaseNote
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

enum class NoteSourceCode { AUTO, EXT, INST }
