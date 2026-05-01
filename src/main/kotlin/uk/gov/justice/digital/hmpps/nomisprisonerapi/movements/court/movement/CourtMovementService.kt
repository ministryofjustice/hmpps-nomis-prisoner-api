package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.movement

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Transactional(readOnly = true)
@Service
class CourtMovementService(
  private val courtMovementOutRepository: OffenderCourtMovementOutRepository,
  private val courtMovementInRepository: OffenderCourtMovementInRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
) {

  fun getCourtMovementOut(offenderNo: String, bookingId: Long, sequence: Int): CourtMovementOut {
    val offenderBooking = findOffenderBookingOrThrow(offenderNo, bookingId)

    return courtMovementOutRepository.findByIdOrNull(OffenderExternalMovementId(offenderBooking, sequence))
      ?.toResponse()
      ?: throw NotFoundException("Court movement out with bookingId=$bookingId and sequence=$sequence not found for offender with nomsId=$offenderNo")
  }

  fun getCourtMovementIn(offenderNo: String, bookingId: Long, sequence: Int): CourtMovementIn {
    val offenderBooking = findOffenderBookingOrThrow(offenderNo, bookingId)

    return courtMovementInRepository.findByIdOrNull(OffenderExternalMovementId(offenderBooking, sequence))
      ?.toResponse()
      ?: throw NotFoundException("Court movement in with bookingId=$bookingId and sequence=$sequence not found for offender with nomsId=$offenderNo")
  }

  private fun findOffenderBookingOrThrow(offenderNo: String, bookingId: Long): OffenderBooking {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    return offenderBookingRepository.findByIdOrNull(bookingId)
      ?.takeIf { it.offender.nomsId == offenderNo }
      ?: throw NotFoundException("Offender booking with bookingId=$bookingId not found for offender with nomsId=$offenderNo")
  }

  private fun OffenderCourtMovementOut.toResponse() = CourtMovementOut(
    bookingId = id.offenderBooking.bookingId,
    sequence = id.sequence,
    courtScheduleOutId = courtScheduleOut?.id,
    movementDate = movementDate,
    movementTime = movementTime,
    movementReason = movementReason.id.reasonCode,
    fromPrison = fromAgency!!.id,
    toCourt = toAgency!!.id,
    commentText = commentText,
    audit = toAudit(),
  )

  private fun OffenderCourtMovementIn.toResponse() = CourtMovementIn(
    bookingId = id.offenderBooking.bookingId,
    sequence = id.sequence,
    courtScheduleOutId = courtScheduleOut?.id,
    movementDate = movementDate,
    movementTime = movementTime,
    movementReason = movementReason.id.reasonCode,
    fromCourt = fromAgency!!.id,
    toPrison = toAgency!!.id,
    commentText = commentText,
    audit = toAudit(),
  )
}
