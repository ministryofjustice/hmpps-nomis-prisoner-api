package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderTemporaryAbsence(
  id: OffenderExternalMovementId,
  movementDate: LocalDate,
  movementTime: LocalDateTime,
  movementType: MovementType,
  movementReason: MovementReason,
  arrestAgency: ArrestAgency? = null,
  escort: Escort? = null,
  escortText: String? = null,
  fromPrison: AgencyLocation,
  toAgency: AgencyLocation? = null,
  active: Boolean = false,
  commentText: String? = null,
  toCity: City? = null,
  toAddress: Address? = null,

  @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "EVENT_ID")
  var scheduledTemporaryAbsence: OffenderScheduledTemporaryAbsence? = null,
) : OffenderExternalMovement(
  id = id,
  movementDate = movementDate,
  movementTime = movementTime,
  movementType = movementType,
  movementReason = movementReason,
  movementDirection = MovementDirection.OUT,
  arrestAgency = arrestAgency,
  escort = escort,
  escortText = escortText,
  fromAgency = fromPrison,
  toAgency = toAgency,
  active = active,
  commentText = commentText,
  toCity = toCity,
  toAddress = toAddress,
) {
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", updatable = false, insertable = false, nullable = false)
  val offenderBooking: OffenderBooking = id.offenderBooking
}
