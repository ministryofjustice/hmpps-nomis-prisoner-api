package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "COURSE_SCHEDULES")
data class CourseSchedule(
  @Id
  @SequenceGenerator(name = "CRS_SCH_ID", sequenceName = "CRS_SCH_ID", allocationSize = 1)
  @GeneratedValue(generator = "CRS_SCH_ID")
  @Column(name = "CRS_SCH_ID", nullable = false)
  val courseScheduleId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID", nullable = false)
  val courseActivity: CourseActivity,

  @Column
  var scheduleDate: LocalDate,

  @Column
  private var startTime: LocalDateTime,

  @Column
  private var endTime: LocalDateTime,

  @Column
  var scheduleStatus: String = "SCH",

  @Column(name = "SLOT_CATEGORY_CODE")
  @Enumerated(EnumType.STRING)
  var slotCategory: SlotCategory? = null,

  @OneToMany(mappedBy = "courseSchedule", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val offenderCourseAttendances: MutableList<OffenderCourseAttendance> = mutableListOf(),
) {
  /**
   * Return the course schedule date with the time portion set to the course schedule start time. Under some
   * circumstances (and until corrected by TAG_DATETIME_CORRECTIONS) the date portion of the course schedule start time
   * may be different, so need to combine the two to ensure we get the correct date and time.
   *
   * @return The combined LocalDateTime representing the course schedule date and start time.
   */
  fun getScheduleDateAndStartTime(): LocalDateTime = scheduleDate.atTime(startTime.toLocalTime())

  /**
   * Return the course schedule date with the time portion set to the course schedule end time. Under some
   * circumstances (and until corrected by TAG_DATETIME_CORRECTIONS) the date portion of the course schedule end time
   * may be different, so need to combine the two to ensure we get the correct date and time.
   *
   * @return The combined LocalDateTime representing the course schedule date and end time.
   */
  fun getScheduleDateAndEndTime(): LocalDateTime = scheduleDate.atTime(endTime.toLocalTime())

  fun setScheduleDateAndTime(startDateTime: LocalDateTime, endDateTime: LocalDateTime) {
    scheduleDate = startDateTime.toLocalDate()
    startTime = startDateTime
    endTime = scheduleDate.atTime(endDateTime.toLocalTime())
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourseSchedule

    return courseScheduleId == other.courseScheduleId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(courseScheduleId = $courseScheduleId, courseActivityId = ${courseActivity.courseActivityId}, scheduleDate = $scheduleDate, startTime = $startTime, endTime = $endTime)"
}
