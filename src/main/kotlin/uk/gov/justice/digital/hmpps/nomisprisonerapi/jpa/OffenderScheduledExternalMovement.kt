package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate
import java.time.LocalDateTime

// TODO SDIT-2872 This isn't being used yet and is missing some details - it will hopefully become abstract with subclasses for the various types of external movement
@Entity
@DiscriminatorValue("EXT_MOV")
class OffenderScheduledExternalMovement(
  eventId: Long = 0,
  offenderBooking: OffenderBooking,
  eventDate: LocalDate? = null,
  startTime: LocalDateTime? = null,
  eventType: String, // One of TRN (transfers), TAP (temporary absence) or CRT (court)
  eventSubType: EventSubType,
  eventStatus: EventStatus,
  prison: AgencyLocation? = null,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_AGY_LOC_ID")
  val toPrison: AgencyLocation? = null,
  comment: String? = null,
) : OffenderIndividualSchedule(
  eventId = eventId,
  offenderBooking = offenderBooking,
  eventDate = eventDate,
  startTime = startTime,
  eventClass = EventClass.EXT_MOV,
  eventType = eventType,
  eventSubType = eventSubType,
  eventStatus = eventStatus,
  prison = prison,
  comment = comment,
)
