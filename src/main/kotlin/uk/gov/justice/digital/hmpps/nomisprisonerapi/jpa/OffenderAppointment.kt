package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
class OffenderAppointment(
  eventId: Long = 0,
  offenderBooking: OffenderBooking,
  eventDate: LocalDate? = null,
  startTime: LocalDateTime? = null,

  @Column
  private var endTime: LocalDateTime? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${InternalScheduleReason.INT_SCH_RSN}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_SUB_TYPE", referencedColumnName = "code")),
    ],
  )
  var eventSubType: InternalScheduleReason,

  eventStatus: EventStatus,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val prison: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_INTERNAL_LOCATION_ID")
  var internalLocation: AgencyInternalLocation? = null,
  comment: String? = null,
) : OffenderIndividualSchedule(
  eventId = eventId,
  offenderBooking = offenderBooking,
  eventDate = eventDate,
  startTime = startTime,
  eventClass = EventClass.INT_MOV,
  eventType = EventType.APP,
  eventStatus = eventStatus,
  comment = comment,
) {
  /**
   * Return the appointment end date with the time portion set to the appointment end time. Under some circumstances
   * (and until corrected by TAG_DATETIME_CORRECTIONS) the date portion of the appointment end time may be different,
   * so need to combine the two to ensure we get the correct date and time.
   *
   * @return The combined LocalDateTime representing the appointment end date and time.
   */
  fun getAppointmentEndDateAndTime(): LocalDateTime? = endTime?.let {
    eventDate!!.atTime(it.toLocalTime())
  }

  fun setAppointmentEndTime(eventTime: LocalTime?) {
    endTime = eventTime?.let { eventDate!!.atTime(eventTime) }
  }
}
