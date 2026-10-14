package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderAdmissionMovementIn(
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
) {
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", updatable = false, insertable = false, nullable = false)
  val offenderBooking: OffenderBooking = id.offenderBooking
}
