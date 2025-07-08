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
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderScheduledTemporaryAbsence(
  eventId: Long = 0,
  offenderBooking: OffenderBooking,
  eventDate: LocalDate? = null,
  startTime: LocalDateTime? = null,
  eventSubType: MovementReason,
  eventStatus: EventStatus,
  comment: String? = null,
  escort: Escort,
  fromPrison: AgencyLocation,
  toAgency: AgencyLocation? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TemporaryAbsenceTransportType.TA_TRANSPORT}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TRANSPORT_CODE", referencedColumnName = "code")),
    ],
  )
  val transportType: TemporaryAbsenceTransportType,

  @JoinColumn(name = "RETURN_DATE")
  val returnDate: LocalDate,

  @JoinColumn(name = "RETURN_TIME")
  val returnTime: LocalDateTime,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_MOVEMENT_APP_ID")
  val temporaryAbsenceApplication: OffenderMovementApplication,

  @Column(name = "TO_ADDRESS_OWNER_CLASS")
  val toAddressOwnerClass: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_ADDRESS_ID")
  val toAddress: Address? = null,

  @Column(name = "APPLICATION_DATE")
  val applicationDate: LocalDateTime,

  @Column(name = "APPLICATION_TIME")
  val applicationTime: LocalDateTime? = null,

  @OneToOne(mappedBy = "scheduledTemporaryAbsence", cascade = [CascadeType.ALL])
  @JoinColumn(name = "EVENT_ID", insertable = false, updatable = false)
  var scheduledTemporaryAbsenceReturn: OffenderScheduledTemporaryAbsenceReturn? = null,

  @OneToOne(mappedBy = "scheduledTemporaryAbsence", cascade = [CascadeType.ALL])
  @JoinColumn(name = "EVENT_ID", insertable = false, updatable = false)
  var temporaryAbsence: OffenderTemporaryAbsence? = null,
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
  direction = MovementDirection.OUT,
  fromAgency = fromPrison,
  toAgency = toAgency,
)
