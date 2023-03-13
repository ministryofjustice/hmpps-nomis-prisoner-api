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
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_COURSE_ATTENDANCES")
class OffenderCourseAttendance(
  @Id
  @Column(name = "EVENT_ID")
  @SequenceGenerator(name = "EVENT_ID", sequenceName = "EVENT_ID", allocationSize = 1)
  @GeneratedValue(generator = "EVENT_ID")
  val eventId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column
  val eventDate: LocalDate,

  @Column
  val startTime: LocalDateTime? = null,

  @Column
  val endTime: LocalDateTime? = null,

  @Column(nullable = false)
  val eventSubType: String = "PA",

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + EventStatus.EVENT_STS + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_STATUS", referencedColumnName = "code")),
    ],
  )
  val eventStatus: EventStatus,

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_INTERNAL_LOCATION_ID")
  val toInternalLocation: AgencyInternalLocation? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_SCH_ID")
  val courseSchedule: CourseSchedule? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AttendanceOutcome.ATTENDANCE_OUTCOME + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "OUTCOME_REASON_CODE", referencedColumnName = "code", nullable = true, updatable = false, insertable = false)),
    ],
  )
  val attendanceOutcome: AttendanceOutcome? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFF_PRGREF_ID")
  val offenderProgramProfile: OffenderProgramProfile? = null,

  @Column
  val inTime: LocalDateTime? = null,

  @Column
  val outTime: LocalDateTime? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID", nullable = false)
  val courseActivity: CourseActivity,

  @Column(nullable = false)
  val eventType: String = "PRISON_ACT",

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val prison: AgencyLocation? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val eventClass: EventClass = EventClass.INT_MOV,

  @Column(name = "UNEXCUSED_ABSENCE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val unexcusedAbsence: Boolean = false,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PROGRAM_ID")
  val program: ProgramService? = null,

  @Column
  var bonusPay: BigDecimal? = null,

  @Column(name = "PAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val paid: Boolean = false,

  @Column(name = "AUTHORISED_ABSENCE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val authorisedAbsence: Boolean = false,
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderCourseAttendance

    return eventId == other.eventId
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String =
    "OffenderCourseAttendance(eventId=$eventId, offenderBookingId=${offenderBooking.bookingId}, courseScheduleId=${courseSchedule?.courseScheduleId}, eventDate=$eventDate, attendanceOutcome=${attendanceOutcome?.code}, paid=$paid)"
}
