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
import java.math.RoundingMode
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
  var eventDate: LocalDate,

  @Column
  var startTime: LocalDateTime? = null,

  @Column
  var endTime: LocalDateTime? = null,

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
  var eventStatus: EventStatus,

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_INTERNAL_LOCATION_ID")
  val toInternalLocation: AgencyInternalLocation? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_SCH_ID")
  val courseSchedule: CourseSchedule,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AttendanceOutcome.ATTENDANCE_OUTCOME + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_OUTCOME", referencedColumnName = "code")),
    ],
  )
  var attendanceOutcome: AttendanceOutcome? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFF_PRGREF_ID")
  val offenderProgramProfile: OffenderProgramProfile,

  @Column
  val inTime: LocalDateTime? = null,

  @Column
  val outTime: LocalDateTime? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID")
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
  var unexcusedAbsence: Boolean? = false,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PROGRAM_ID")
  var program: ProgramService? = null,

  bonusPay: BigDecimal? = null,

  @Column(name = "PAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  var pay: Boolean? = null,

  @Column(name = "AUTHORISED_ABSENCE_FLAG")
  @Convert(converter = YesNoConverter::class)
  var authorisedAbsence: Boolean? = false,

  @Column
  var commentText: String? = null,

  @Column(name = "TXN_ID", updatable = false)
  val paidTransactionId: Long? = null,

  @Column(name = "REFERENCE_ID")
  val referenceId: Long? = null,

  @Column(name = "PERFORMANCE_CODE")
  var performanceCode: String? = null,
) : Serializable {

  @Column
  var bonusPay = bonusPay?.setScale(3, RoundingMode.HALF_UP)
    set(value) { field = value?.setScale(3, RoundingMode.HALF_UP) }

  fun isPaid() = paidTransactionId != null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderCourseAttendance

    return eventId == other.eventId
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String =
    "OffenderCourseAttendance(eventId=$eventId, offenderBookingId=${offenderBooking.bookingId}, courseScheduleId=${courseSchedule.courseScheduleId}, eventDate=$eventDate, attendanceOutcome=${attendanceOutcome?.code}, paid=$pay)"
}
