package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.DiscriminatorFormula
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class EventClass {
  EXT_MOV,
  INT_MOV,
  COMM,
}

enum class EventType {
  APP,
  TAP,
}

@EntityOpen
@Entity
@Table(name = "OFFENDER_IND_SCHEDULES")
@DiscriminatorFormula(
  """
    case
        when EVENT_CLASS = 'INT_MOV' then 'OffenderAppointment'
        when EVENT_CLASS = 'EXT_MOV' and EVENT_TYPE = 'TAP' and DIRECTION_CODE = 'OUT' then 'OffenderTapScheduleOut'
        when EVENT_CLASS = 'EXT_MOV' and EVENT_TYPE = 'TAP' and DIRECTION_CODE = 'IN' then 'OffenderTapScheduleIn'
        else 'Unknown'
    end
""",
)
@Inheritance
abstract class OffenderIndividualSchedule(

  @Id
  @SequenceGenerator(name = "EVENT_ID", sequenceName = "EVENT_ID", allocationSize = 1)
  @GeneratedValue(generator = "EVENT_ID")
  @Column(name = "EVENT_ID", nullable = false)
  val eventId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column
  var eventDate: LocalDate? = null,

  @Column
  private var startTime: LocalDateTime? = null,

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_CLASS")
  val eventClass: EventClass,

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_TYPE")
  val eventType: EventType,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${EventStatus.EVENT_STS}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_STATUS", referencedColumnName = "code")),
    ],
  )
  var eventStatus: EventStatus,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,
) : NomisAuditableEntityBasic() {
  /**
   * Return the appointment start date with the time portion set to the appointment start time. Under some circumstances
   * (and until corrected by TAG_DATETIME_CORRECTIONS) the date portion of the appointment start time may be different,
   * so need to combine the two to ensure we get the correct date and time.
   *
   * @return The combined LocalDateTime representing the appointment start date and time.
   */
  fun getAppointmentStartDateAndTime(): LocalDateTime? = startTime?.let {
    eventDate!!.atTime(it.toLocalTime())
  }

  fun setAppointmentStartDateAndTime(eventDate: LocalDate, startTime: LocalTime) {
    this.eventDate = eventDate
    this.startTime = eventDate.atTime(startTime)
  }

  fun setAppointmentStartDateAndTime(eventDate: LocalDate, startTime: LocalDateTime) {
    setAppointmentStartDateAndTime(eventDate, startTime.toLocalTime())
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderIndividualSchedule

    return eventId == other.eventId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $eventId, booking = ${offenderBooking.bookingId}, eventDate = $eventDate, startTime = $startTime)"
}
