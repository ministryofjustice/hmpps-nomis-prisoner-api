package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter

@Entity
@Table(name = "PROGRAM_SERVICES")
data class ProgramService(
  @Id
  @SequenceGenerator(name = "PROGRAM_ID", sequenceName = "PROGRAM_ID", allocationSize = 1)
  @GeneratedValue(generator = "PROGRAM_ID")
  @Column(nullable = false)
  val programId: Long = 0,

  @Column
  val programCode: String,

  @Column(nullable = false)
  val description: String,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val active: Boolean,

  @Column(nullable = false)
  val programClass: String = "PRG",

  @OneToMany(mappedBy = "program", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val courseActivities: MutableList<CourseActivity> = mutableListOf(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ProgramService

    return programId == other.programId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(programId = $programId )"
  }
}

/*
other columns:
"PROGRAM_CATEGORY"	"VARCHAR2(12 CHAR)"	"Yes"	""	2	"Program category derived from reference code ""PS_CATEGORY"""
"EXPIRY_DATE"	"DATE"	"Yes"	""	6	"Set to date on which the P/S becomes inactive"
"COMMENT_TEXT"	"VARCHAR2(240 CHAR)"	"Yes"	""	7	"The general comment text"
"PARENT_PROGRAM_ID"	"NUMBER(10,0)"	"Yes"	""	9	"The parent of the program class"
"CONTACT_METHOD"	"VARCHAR2(80 CHAR)"	"Yes"	""	10	"Method of contact in free text"
"NO_OF_SESSIONS"	"NUMBER(6,0)"	"Yes"	""	11	"Number of weekly sessions"
"NO_OF_ALLOWABLE_RESTARTS"	"NUMBER(6,0)"	"Yes"	""	12	"Number of allowable restart for the program"
"NO_OF_ALLOWABLE_ABSENCES"	"NUMBER(6,0)"	"Yes"	""	13	"Number of allowable absence for the program"
"CAPACITY"	"NUMBER(6,0)"	"Yes"	""	14	"The capacity"
"SESSION_LENGTH"	"NUMBER(6,0)"	"Yes"	""	15	"The session length in hours"
"COMPLETION_FLAG"	"VARCHAR2(1 CHAR)"	"Yes"	"'N'"	16	"The phase which decide when the services is considered as completed"
"MODULE_FLAG"	"VARCHAR2(1 CHAR)"	"Yes"	"'N'"	17	"If there are module for the program phase"
"MODULE_TYPE"	"VARCHAR2(12 CHAR)"	"Yes"	""	18	"The module type of the program phase. Reference Code(PS_MOD_TYPE)"
"BREAK_ALLOWED_FLAG"	"VARCHAR2(1 CHAR)"	"Yes"	"'N'"	19	"?Allow break"
"START_FLAG"	"VARCHAR2(1 CHAR)"	"Yes"	"'N'"	20	"?Can start at this module"
"NO_OF_WEEKLY_SESSIONS"	"NUMBER(6,0)"	"Yes"	""	21	"No of session per week"
"PROGRAM_STATUS"	"VARCHAR2(12 CHAR)"	"Yes"	""	22	"Reference Code(PS_STATUS)"
"LIST_SEQ"	"NUMBER(6,0)"	"Yes"	""	23	"The listing order"
"PHASE_TYPE"	"VARCHAR2(12 CHAR)"	"Yes"	""	24	"The phase type.  Reference Code(PS_PHS_TYPE)"
"START_DATE"	"DATE"	"Yes"	""	25	"The start date"
"END_DATE"	"DATE"	"Yes"	""	26	"The End date"
"FUNCTION_TYPE"	"VARCHAR2(12 CHAR)"	"Yes"	""	38	""
 */
