package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@EntityOpen
@Entity
abstract class OffenderScheduledExternalMovement(
  eventId: Long = 0,
  offenderBooking: OffenderBooking,
  eventDate: LocalDate? = null,
  startTime: LocalDateTime? = null,
  eventType: EventType,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val fromAgency: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_AGY_LOC_ID")
  val toAgency: AgencyLocation? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${MovementReason.MOVE_RSN}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_SUB_TYPE", referencedColumnName = "code")),
    ],
  )
  var eventSubType: MovementReason,

  eventStatus: EventStatus,
  comment: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${Escort.ESCORT}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "ESCORT_CODE", referencedColumnName = "code")),
    ],
  )
  val escort: Escort,

  @Enumerated(EnumType.STRING)
  @Column(name = "DIRECTION_CODE")
  val direction: MovementDirection,
) : OffenderIndividualSchedule(
  eventId = eventId,
  offenderBooking = offenderBooking,
  eventDate = eventDate,
  startTime = startTime,
  eventClass = EventClass.EXT_MOV,
  eventType = eventType,
  eventStatus = eventStatus,
  comment = comment,
)
