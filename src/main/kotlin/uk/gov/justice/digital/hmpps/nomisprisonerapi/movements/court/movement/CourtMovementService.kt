package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.movement

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Transactional(readOnly = true)
@Service
class CourtMovementService(
  private val courtMovementOutRepository: OffenderCourtMovementOutRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
) {

  fun getCourtMovementOut(offenderNo: String, bookingId: Long, sequence: Int): CourtMovementOut {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }
    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?.takeIf { it.offender.nomsId == offenderNo }
      ?: throw NotFoundException("Offender booking with bookingId=$bookingId not found for offender with nomsId=$offenderNo")

    return courtMovementOutRepository.findByIdOrNull(OffenderExternalMovementId(offenderBooking, sequence))
      ?.toResponse()
      ?: throw NotFoundException("Court movement with bookingId=$bookingId and sequence=$sequence not found for offender with nomsId=$offenderNo")
  }

  // TODO implement this method
  fun getCourtMovementIn(offenderNo: String, bookingId: Long, sequence: Int) = CourtMovementIn(
    bookingId = bookingId,
    sequence = sequence,
    courtScheduleOutId = null,
    movementDate = LocalDate.now(),
    movementTime = LocalDateTime.now(),
    movementReason = "COURT",
    fromCourt = "LEEDMC",
    toPrison = "LEI",
    commentText = "Court movement",
    audit = NomisAudit(createUsername = "USER", createDatetime = LocalDateTime.now()),
  )

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
}
