package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
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

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TransferCancellationReason.TRANSFER_CANCELLATION_REASON}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "OUTCOME_REASON_CODE", referencedColumnName = "code")),
    ],
  )
  var cancellationReasonCode: TransferCancellationReason? = null,

  @OneToOne(mappedBy = "schedule", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @JoinColumn(name = "EVENT_ID", insertable = false, updatable = false)
  var waitList: OffenderTransferScheduleWaitList? = null,

  @Column(name = "HIDDEN_COMMENT_TEXT")
  var hiddenComment: String? = null,
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
