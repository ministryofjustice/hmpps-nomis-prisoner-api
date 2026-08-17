package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.movement

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers

@Transactional(readOnly = true)
@Service
class TransferMovementService(
  private val transferMovementOutRepository: OffenderTransferMovementOutRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val transferScheduleOutRepository: OffenderTransferScheduleOutRepository,
  private val movementHelpers: MovementHelpers,
) {

  fun getTransferMovementOut(offenderNo: String, bookingId: Long, sequence: Int): TransferMovementOut {
    val offenderBooking = findOffenderBookingOrThrow(offenderNo, bookingId)

    return transferMovementOutRepository.findByIdOrNull(OffenderExternalMovementId(offenderBooking, sequence))
      ?.toResponse()
      ?: throw NotFoundException("Transfer movement out with bookingId=$bookingId and sequence=$sequence not found for offender with nomsId=$offenderNo")
  }

  private fun findOffenderBookingOrThrow(offenderNo: String, bookingId: Long): OffenderBooking {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    return offenderBookingRepository.findByIdOrNull(bookingId)
      ?.takeIf { it.offender.nomsId == offenderNo }
      ?: throw NotFoundException("Offender booking with bookingId=$bookingId not found for offender with nomsId=$offenderNo")
  }

  private fun OffenderTransferMovementOut.toResponse(): TransferMovementOut {
    val transferScheduleOut = transferScheduleOutId
      ?.let { transferScheduleOutRepository.findByIdOrNull(it) }
      ?.takeIf { it.offenderBooking.bookingId == id.offenderBooking.bookingId }
    return TransferMovementOut(
      bookingId = id.offenderBooking.bookingId,
      sequence = id.sequence,
      eventId = transferScheduleOutId,
      transferScheduleOutId = transferScheduleOut?.eventId,
      movementTime = getMovementDateAndTime(),
      movementReason = movementReason.id.reasonCode,
      escort = escort?.code,
      fromPrison = fromAgency!!.id,
      toPrison = toAgency!!.id,
      active = active,
      commentText = commentText,
      audit = toAudit(),
      userActiveCaseloadId = movementHelpers.activeCaseloadId(modifyUserId ?: createUsername),
    )
  }
}
