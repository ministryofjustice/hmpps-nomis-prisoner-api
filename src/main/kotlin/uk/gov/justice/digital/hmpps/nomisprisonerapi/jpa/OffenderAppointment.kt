package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@DiscriminatorValue("INT_MOV")
class OffenderAppointment(
  eventId: Long = 0,
  offenderBooking: OffenderBooking,
  eventDate: LocalDate? = null,
  startTime: LocalDateTime? = null,
  @Column
  var endTime: LocalDateTime? = null,
  eventSubType: EventSubType,
  eventStatus: EventStatus,
  prison: AgencyLocation? = null,
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
  eventType = "APP",
  eventSubType = eventSubType,
  eventStatus = eventStatus,
  prison = prison,
  comment = comment,
)
