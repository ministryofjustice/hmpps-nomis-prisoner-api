package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderCourtMovementIn(
  id: OffenderExternalMovementId,
  movementDate: LocalDate,
  movementTime: LocalDateTime,
  movementReason: MovementTypeAndReason,
  fromCourt: AgencyLocation? = null,
  toPrison: AgencyLocation,
  active: Boolean = false,
  commentText: String? = null,

  @OneToOne()
  @JoinColumn(name = "PARENT_EVENT_ID")
  @NotFound(action = NotFoundAction.IGNORE)
  var courtScheduleOut: CourtEvent? = null,
) : OffenderExternalMovement(
  id = id,
  movementDate = movementDate,
  movementTime = movementTime,
  movementReason = movementReason,
  movementDirection = MovementDirection.IN,
  arrestAgency = null,
  escort = null,
  escortText = null,
  fromAgency = fromCourt,
  toAgency = toPrison,
  active = active,
  commentText = commentText,
  fromCity = null,
  fromAddress = null,
) {
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", updatable = false, insertable = false, nullable = false)
  val offenderBooking: OffenderBooking = id.offenderBooking
}
