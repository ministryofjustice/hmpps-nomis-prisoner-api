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
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_PROGRAM_PROFILES")
data class OffenderProgramProfile(
  @Id
  @Column(name = "OFF_PRGREF_ID")
  @SequenceGenerator(name = "OFF_PRGREF_ID", sequenceName = "OFF_PRGREF_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFF_PRGREF_ID")
  val offenderProgramReferenceId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PROGRAM_ID", nullable = false)
  val program: ProgramService,

  @Column(name = "OFFENDER_START_DATE")
  val startDate: LocalDate? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + OffenderProgramStatus.OFFENDER_PROGRAM_STATUS + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "OFFENDER_PROGRAM_STATUS", referencedColumnName = "code"))
    ]
  )
  val programStatus: OffenderProgramStatus,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID")
  val courseActivity: CourseActivity? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val prison: AgencyLocation? = null,

  @Column(name = "OFFENDER_END_DATE")
  var endDate: LocalDate? = null,

  @OneToMany(mappedBy = "offenderProgramProfile", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val payBands: MutableList<OffenderProgramProfilePayBand> = mutableListOf(),

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + ProgramServiceEndReason.END_REASON + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "OFFENDER_END_REASON", referencedColumnName = "code"))
    ]
  )
  var endReason: ProgramServiceEndReason? = null,

  @Column(name = "OFFENDER_END_COMMENT_TEXT")
  var endComment: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderProgramProfile
    return offenderProgramReferenceId == other.offenderProgramReferenceId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  fun isCurrentActivity(): Boolean {
    val currentDate = LocalDate.now()
    val isCurrentProgramProfile = programStatus.code == "ALLOC" &&
      startAndEndDatesSpanDay(startDate, endDate, currentDate)
    val isCurrentCourseActivity = courseActivity != null &&
      startAndEndDatesSpanDay(courseActivity.scheduleStartDate, courseActivity.scheduleEndDate, currentDate)
    return isCurrentProgramProfile && isCurrentCourseActivity
  }

  fun startAndEndDatesSpanDay(startDate: LocalDate?, endDate: LocalDate?, dateToCheck: LocalDate): Boolean =
    isDateBefore(startDate, dateToCheck.plusDays(1)) && isDateAfter(endDate, dateToCheck)

  fun isDateBefore(date: LocalDate?, comparedDate: LocalDate) = date?.isBefore(comparedDate) ?: false
  fun isDateAfter(date: LocalDate?, comparedDate: LocalDate) = date?.isAfter(comparedDate) ?: false
}
