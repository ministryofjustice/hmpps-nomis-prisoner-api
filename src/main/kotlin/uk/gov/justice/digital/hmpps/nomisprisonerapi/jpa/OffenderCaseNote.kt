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
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
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

private const val AMEND_CASE_NOTE_FORMAT = "%s ...[%s updated the case notes on %s] %s"

@Entity
@Table(name = "OFFENDER_CASE_NOTES")
@EntityOpen
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
  val occurrenceDate: LocalDate? = null,

  @Column(name = "CONTACT_TIME")
  var occurrenceDateTime: LocalDateTime? = null,
  // date part always the same as CONTACT_DATE

  @ManyToOne(optional = false)
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

  @ManyToOne(optional = false)
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

  @Column(name = "CASE_NOTE_TEXT")
  var caseNoteText: String? = null,

  @Column(name = "AMENDMENT_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val amendmentFlag: Boolean = false,

  // IWP_FLAG - not used
  // CHECK BOX 1 to 5 - not used
  // EVENT_ID - not used

  @Column(name = "NOTE_SOURCE_CODE")
  @Enumerated(EnumType.STRING)
  var noteSourceCode: NoteSourceCode? = null,

  @Column(name = "DATE_CREATION")
  @CreatedDate
  var dateCreation: LocalDate? = null,
  // can be different to both the contact date and the create_datetime

  @Column(name = "TIME_CREATION")
  @CreatedDate
  var timeCreation: LocalDateTime? = null,
  // date part always the same as DATE_CREATION

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
