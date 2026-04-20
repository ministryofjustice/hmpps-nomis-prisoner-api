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
class OffenderTapMovementIn(
  id: OffenderExternalMovementId,
  movementDate: LocalDate,
  movementTime: LocalDateTime,
  movementReason: MovementTypeAndReason,
  arrestAgency: ArrestAgency? = null,
  escort: Escort? = null,
  escortText: String? = null,
  fromAgency: AgencyLocation? = null,
  toPrison: AgencyLocation,
  active: Boolean = false,
  commentText: String? = null,
  fromCity: City? = null,
  fromAddress: Address? = null,

  @OneToOne()
  @JoinColumn(name = "EVENT_ID")
  @NotFound(action = NotFoundAction.IGNORE)
  var tapScheduleIn: OffenderTapScheduleIn? = null,

  @OneToOne()
  @JoinColumn(name = "PARENT_EVENT_ID")
  @NotFound(action = NotFoundAction.IGNORE)
  var tapScheduleOut: OffenderTapScheduleOut? = null,
) : OffenderExternalMovement(
  id = id,
  movementDate = movementDate,
  movementTime = movementTime,
  movementReason = movementReason,
  movementDirection = MovementDirection.IN,
  arrestAgency = arrestAgency,
  escort = escort,
  escortText = escortText,
  fromAgency = fromAgency,
  toAgency = toPrison,
  active = active,
  commentText = commentText,
  fromCity = fromCity,
  fromAddress = fromAddress,
) {
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", updatable = false, insertable = false, nullable = false)
  val offenderBooking: OffenderBooking = id.offenderBooking
}
