package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.IN
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementOutRepository
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
  private val tapMovementOutRepository: OffenderTapMovementOutRepository,
  private val tapMovementInRepository: OffenderTapMovementInRepository,
) {
  fun getOffenderPersonLocations(offenderNo: String): OffenderPersonLocationsResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }
    // Loads TAP movements with their schedules and addresses in one go (the entity associations are eager) so the
    // booking's movements below are resolved from the persistence context rather than one query per movement
    tapMovementOutRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
    tapMovementInRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
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
            .filterNot {
              it.isInvalidTypeAndDirection()
                .also { filtered -> if (filtered) log.warn("Ignoring person location movement ${it.id} because its type/direction of ${it.movementReason.id.type}/${it.movementDirection} does not make sense.") }
            }
            .sortedBy { it.id.sequence }
            .map { it.toPersonLocationMovement() },
          audit = booking.toAudit(),
        )
      },
    )
  }

  private fun OffenderExternalMovement.isInvalidTypeAndDirection() = (movementDirection == IN && movementReason.id.type in ["REL", "TRN"]) ||
    (movementDirection == OUT && movementReason.id.type == "ADM")

  private fun OffenderExternalMovement.toPersonLocationMovement(): PersonLocationMovement {
    val movementType = toMovementType()
    val scheduleTapOut = (this as? OffenderTapMovementOut)?.tapScheduleOut
    val parentScheduleTapOut = (this as? OffenderTapMovementIn)?.tapScheduleOut
    return PersonLocationMovement(
      sequence = id.sequence,
      movementType = movementType,
      directionCode = MovementDirection.valueOf(movementDirection.name),
      movementTime = getMovementDateAndTime(),
      movementReason = CodeDescription(movementReason.id.reasonCode, movementReason.description),
      from = when {
        unknownFrom(movementType) -> MovementLocation("UNKNOWN", OTHER, "Unknown")
        else -> fromAgency.toMovementLocation(
          address = if (isTap()) fromAddress ?: parentScheduleTapOut?.toAddress else null,
          city = if (isTap()) fromCity else null,
        )
      },
      to = toAgency.toMovementLocation(
        address = if (isTap()) toAddress ?: scheduleTapOut?.toAddress else null,
        city = if (isTap()) toCity else null,
      ),
      scheduleId = if (this is OffenderCourtMovementIn) courtScheduleOutId else eventId,
      commentText = commentText,
      audit = toAudit(),
    )
  }

  private fun OffenderExternalMovement.toMovementType() = when (this) {
    is OffenderAdmissionMovementIn -> MovementType.ADM
    is OffenderReleaseMovementOut -> MovementType.REL
    is OffenderTransferMovementOut -> MovementType.TRN
    is OffenderTapMovementOut, is OffenderTapMovementIn -> MovementType.TAP
    is OffenderCourtMovementOut, is OffenderCourtMovementIn -> MovementType.CRT
    // Outbound transfers with no from location are excluded from the entity discriminator so arrive as the base class
    else -> if (movementDirection.name == "OUT" && movementReason.id.type == "TRN") {
      MovementType.TRN
    } else {
      throw IllegalStateException("Unknown movement type ${this::class} for movement $id")
    }
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

  private fun OffenderExternalMovement.isTap() = this is OffenderTapMovementOut || this is OffenderTapMovementIn

  private fun OffenderExternalMovement.unknownFrom(movementType: MovementType) = fromAgency == null && movementDirection.name == "OUT" && (movementType in [MovementType.REL, MovementType.TRN])

  companion object {
    private val log = LoggerFactory.getLogger(OffenderExternalMovement::class.java)
  }
}
