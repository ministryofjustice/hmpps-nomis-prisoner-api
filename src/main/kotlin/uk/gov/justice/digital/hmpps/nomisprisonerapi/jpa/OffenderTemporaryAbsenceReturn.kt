package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class OffenderTemporaryAbsenceReturn(
  id: OffenderExternalMovementId,
  movementDate: LocalDate,
  movementTime: LocalDateTime,
  movementType: MovementType,
  movementReason: MovementReason,
  arrestAgency: ArrestAgency? = null,
  escort: Escort? = null,
  escortText: String? = null,
  fromAgency: AgencyLocation? = null,
  toPrison: AgencyLocation? = null,
  active: Boolean = false,
  commentText: String? = null,
  fromCity: City? = null,
  fromAddress: Address? = null,

  @OneToOne()
  @JoinColumn(name = "EVENT_ID")
  var scheduledTemporaryAbsenceReturn: OffenderScheduledTemporaryAbsenceReturn? = null,

  @OneToOne()
  @JoinColumn(name = "PARENT_EVENT_ID")
  var scheduledTemporaryAbsence: OffenderScheduledTemporaryAbsence? = null,

  @OneToOne
  @JoinColumn(name = "FROM_ADDRESS_ID", insertable = false, updatable = false)
  val fromAddressView: OffenderExternalMovementAddress? = null,
) : OffenderExternalMovement(
  id = id,
  movementDate = movementDate,
  movementTime = movementTime,
  movementType = movementType,
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
