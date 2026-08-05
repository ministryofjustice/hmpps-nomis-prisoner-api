package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderTransferScheduleOut(
  eventId: Long = 0,
  offenderBooking: OffenderBooking,
  eventDate: LocalDate? = null,
  startTime: LocalDateTime? = null,
  eventSubType: MovementReason,
  eventStatus: EventStatus,
  comment: String? = null,
  escort: Escort? = null,
  fromPrison: AgencyLocation,
  toPrison: AgencyLocation? = null,

  @OneToOne(mappedBy = "schedule", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @JoinColumn(name = "EVENT_ID", insertable = false, updatable = false)
  @NotFound(action = NotFoundAction.IGNORE)
  var waitList: OffenderTransferScheduleWaitList? = null,
) : OffenderScheduledExternalMovement(
  eventId = eventId,
  offenderBooking = offenderBooking,
  eventDate = eventDate,
  startTime = startTime,
  eventType = EventType.TRN,
  eventSubType = eventSubType,
  eventStatus = eventStatus,
  comment = comment,
  escort = escort,
  direction = MovementDirection.OUT,
  fromAgency = fromPrison,
  toAgency = toPrison,
)
