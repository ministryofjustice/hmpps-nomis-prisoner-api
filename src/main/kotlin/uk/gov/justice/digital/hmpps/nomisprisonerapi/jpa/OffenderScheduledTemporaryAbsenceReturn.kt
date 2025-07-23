package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderScheduledTemporaryAbsenceReturn(
  eventId: Long = 0,
  offenderBooking: OffenderBooking,
  eventDate: LocalDate? = null,
  startTime: LocalDateTime? = null,
  eventSubType: MovementReason,
  eventStatus: EventStatus,
  comment: String? = null,
  escort: Escort,
  fromAgency: AgencyLocation? = null,
  toPrison: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_EVENT_ID")
  val scheduledTemporaryAbsence: OffenderScheduledTemporaryAbsence,

  @OneToOne(mappedBy = "scheduledTemporaryAbsenceReturn", cascade = [CascadeType.ALL])
  @JoinColumn(name = "EVENT_ID", insertable = false, updatable = false)
  var temporaryAbsenceReturn: OffenderTemporaryAbsenceReturn? = null,

  // This will only exist for merged records, and we use it to find the correct application for this scheduled return
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_MOVEMENT_APP_ID")
  var temporaryAbsenceApplication: OffenderMovementApplication? = null,
) : OffenderScheduledExternalMovement(
  eventId = eventId,
  offenderBooking = offenderBooking,
  eventDate = eventDate,
  startTime = startTime,
  eventType = EventType.TAP,
  eventSubType = eventSubType,
  eventStatus = eventStatus,
  comment = comment,
  escort = escort,
  direction = MovementDirection.IN,
  fromAgency = fromAgency,
  toAgency = toPrison,
)
