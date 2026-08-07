package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderTransferMovementOut(
  id: OffenderExternalMovementId,
  movementDate: LocalDate,
  movementTime: LocalDateTime,
  movementReason: MovementTypeAndReason,
  escort: Escort? = null,
  fromPrison: AgencyLocation,
  toPrison: AgencyLocation? = null,
  active: Boolean = false,
  commentText: String? = null,

  @Column(name = "EVENT_ID")
  var transferScheduleOutId: Long? = null,
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
  toAgency = toPrison,
  active = active,
  commentText = commentText,
  toCity = null,
  toAddress = null,
) {
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", updatable = false, insertable = false, nullable = false)
  val offenderBooking: OffenderBooking = id.offenderBooking
}
