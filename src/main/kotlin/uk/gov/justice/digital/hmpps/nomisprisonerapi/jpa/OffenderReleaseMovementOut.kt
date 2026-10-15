package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderReleaseMovementOut(
  id: OffenderExternalMovementId,
  movementDate: LocalDate,
  movementTime: LocalDateTime,
  movementReason: MovementTypeAndReason,
  escort: Escort? = null,
  fromPrison: AgencyLocation,
  toAgency: AgencyLocation? = null,
  active: Boolean = false,
  commentText: String? = null,
) : OffenderExternalMovement(
  id = id,
  movementDate = movementDate,
  movementTime = movementTime,
  movementReason = movementReason,
  movementDirection = MovementDirection.OUT,
  arrestAgency = null,
  escort = escort,
  escortText = null,
  fromAgency = fromPrison,
  toAgency = toAgency,
  active = active,
  commentText = commentText,
  toCity = null,
  toAddress = null,
) {
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", updatable = false, insertable = false, nullable = false)
  val offenderBooking: OffenderBooking = id.offenderBooking
}
