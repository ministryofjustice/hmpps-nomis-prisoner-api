package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.OUT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdmissionMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderReleaseMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
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
  private val offenderRepository: OffenderRepository,
) {
  fun getOffenderPersonLocations(offenderNo: String): OffenderPersonLocationsResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }
    return OffenderPersonLocationsResponse(
      bookings = offenderBookingRepository.findAllByOffenderNomsId(offenderNo).map { booking ->
        BookingPersonLocations(
          bookingId = booking.bookingId,
          bookingNumber = booking.bookNumber,
          activeBooking = booking.active,
          latestBooking = booking.bookingSequence == 1,
          bookingStatus = if (booking.bookingStatus == "O") OPEN else CLOSED,
          bookingBeginTime = booking.bookingBeginDate,
          bookingEndTime = booking.bookingEndDate,
          movements = booking.externalMovements
            .filterNot { it.createUsername == "SYS" && it.auditModuleName == "MERGE" }
            .sortedBy { it.id.sequence }
            .map { it.toPersonLocationMovement() },
          audit = booking.toAudit(),
        )
      },
    )
  }

  private fun OffenderExternalMovement.toPersonLocationMovement(): PersonLocationMovement {
    val movementType = toMovementType()
    val unknownFrom = fromAgency == null && movementDirection == OUT && (movementType == MovementType.REL || movementType == MovementType.TRN)
    val scheduleOut = (this as? OffenderTapMovementOut)?.tapScheduleOut
    val parentScheduleOut = (this as? OffenderTapMovementIn)?.tapScheduleOut
    return PersonLocationMovement(
      sequence = id.sequence,
      movementType = movementType,
      directionCode = MovementDirection.valueOf(movementDirection.name),
      movementTime = getMovementDateAndTime(),
      movementReason = CodeDescription(movementReason.id.reasonCode, movementReason.description),
      from = when {
        unknownFrom -> MovementLocation("UNKNOWN", OTHER, "Unknown")
        else -> fromAgency.toMovementLocation(
          address = if (isTap()) fromAddress ?: parentScheduleOut?.toAddress else null,
          city = if (isTap()) fromCity else null,
        )
      },
      to = toAgency.toMovementLocation(
        address = if (isTap()) toAddress ?: scheduleOut?.toAddress else null,
        city = if (isTap()) toCity else null,
      ),
      scheduleId = if (this is OffenderCourtMovementIn) courtScheduleOutId else eventId,
      commentText = commentText,
      audit = toAudit(),
    )
  }

  private fun OffenderExternalMovement.toMovementType() = when {
    this is OffenderAdmissionMovementIn -> MovementType.ADM
    this is OffenderReleaseMovementOut -> MovementType.REL
    this is OffenderTransferMovementOut -> MovementType.TRN
    this is OffenderTapMovementOut || this is OffenderTapMovementIn -> MovementType.TAP
    this is OffenderCourtMovementOut || this is OffenderCourtMovementIn -> MovementType.CRT
    // Outbound transfers with no from location are excluded from the entity discriminator so arrive as the base class
    movementDirection == OUT && movementReason.id.type == "TRN" -> MovementType.TRN
    else -> throw IllegalStateException("Unknown movement type ${this::class} for movement $id")
  }

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
