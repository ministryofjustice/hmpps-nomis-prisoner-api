package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdmissionMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderReleaseMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.BookingStatus.CLOSED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.BookingStatus.OPEN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementLocationType.COURT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementLocationType.OTHER
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementLocationType.PRISON
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.toFullAddress

@Service
@Transactional(readOnly = true)
class OffenderPersonLocationsService(
  private val offenderBookingRepository: OffenderBookingRepository,
) {
  fun getOffenderPersonLocations(offenderNo: String): OffenderPersonLocationsResponse = OffenderPersonLocationsResponse(
    bookings = offenderBookingRepository.findAllByOffenderNomsId(offenderNo).map { booking ->
      BookingPersonLocations(
        bookingId = booking.bookingId,
        bookingNumber = booking.bookNumber,
        activeBooking = booking.active,
        latestBooking = booking.bookingSequence == 1,
        bookingStatus = if (booking.bookingStatus == "O") OPEN else CLOSED,
        bookingBeginTime = booking.bookingBeginDate,
        bookingEndTime = booking.bookingEndDate,
        movements = booking.externalMovements.map { movement ->
          PersonLocationMovement(
            sequence = movement.id.sequence,
            movementType = when (movement) {
              is OffenderAdmissionMovementIn -> MovementType.ADM
              is OffenderReleaseMovementOut -> MovementType.REL
              is OffenderTransferMovementOut -> MovementType.TRN
              is OffenderTapMovementOut, is OffenderTapMovementIn -> MovementType.TAP
              is OffenderCourtMovementOut, is OffenderCourtMovementIn -> MovementType.CRT
              else -> throw IllegalStateException("Unknown movement type ${movement::class} for movement ${movement.id}")
            },
            directionCode = MovementDirection.valueOf(movement.movementDirection.name),
            movementTime = movement.getMovementDateAndTime(),
            movementReason = CodeDescription(
              movement.movementReason.id.reasonCode,
              movement.movementReason.description,
            ),
            from = movement.fromAgency?.toMovementLocation(
              address = if (movement.isTap()) movement.fromAddress else null,
              city = if (movement.isTap()) movement.fromCity else null,
            ),
            to = movement.toAgency?.toMovementLocation(
              address = if (movement.isTap()) movement.toAddress else null,
              city = if (movement.isTap()) movement.toCity else null,
            ),
            scheduleId = movement.eventId,
            commentText = movement.commentText,
            audit = movement.toAudit(),
          )
        },
        audit = booking.toAudit(),
      )
    },
  )

  private fun AgencyLocation?.toMovementLocation(address: Address? = null, city: City? = null) = this?.let {
    MovementLocation(
      code = it.id,
      description = it.description,
      type = when {
        it.id.isOtherLocationType() -> OTHER
        it.type.isPrison -> PRISON
        it.type.isCourt -> COURT
        else -> OTHER
      },
    )
  }
    ?: address?.let { MovementLocation(description = it.toFullAddress(), type = OTHER) }
    ?: city?.let { MovementLocation(description = it.description, type = OTHER) }

  private fun String.isOtherLocationType() = this == "OUT" || this == "TRN"
}
