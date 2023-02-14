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
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.time.LocalDateTime

@Entity
@Table(name = "COURSE_SCHEDULE_RULES")
data class CourseScheduleRule(

  @Id
  @SequenceGenerator(name = "COURSE_SCHEDULE_RULE_ID", sequenceName = "COURSE_SCHEDULE_RULE_ID", allocationSize = 1)
  @GeneratedValue(generator = "COURSE_SCHEDULE_RULE_ID")
  @Column(name = "COURSE_SCHEDULE_RULE_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "CRS_ACTY_ID", nullable = false)
  val courseActivity: CourseActivity,

  val weekNo: Int? = 1, // always set to 1 in prod

  @Column(name = "MONDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val monday: Boolean = false,

  @Column(name = "TUESDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val tuesday: Boolean = false,

  @Column(name = "WEDNESDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val wednesday: Boolean = false,

  @Column(name = "THURSDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val thursday: Boolean = false,

  @Column(name = "FRIDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val friday: Boolean = false,

  @Column(name = "SATURDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val saturday: Boolean = false,

  @Column(name = "SUNDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val sunday: Boolean = false,

  @Column
  val startTime: LocalDateTime,

  @Column
  val endTime: LocalDateTime? = null,

  @Column
  @Enumerated(EnumType.STRING)
  val slotCategoryCode: SlotCategory,

) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourseScheduleRule

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
  override fun toString(): String =
    "CourseScheduleRule(id=$id, courseActivityId=${courseActivity.courseActivityId}, startTime=$startTime)"
}

enum class SlotCategory { AM, PM, ED }
