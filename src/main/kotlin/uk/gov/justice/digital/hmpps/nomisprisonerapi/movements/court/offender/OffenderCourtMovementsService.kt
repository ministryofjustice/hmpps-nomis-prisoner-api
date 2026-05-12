package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.offender

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository

@Service
@Transactional
class OffenderCourtMovementsService(
  private val courtEventRepository: CourtEventRepository,
  private val courtMovementOutRepository: OffenderCourtMovementOutRepository,
  private val courtMovementInRepository: OffenderCourtMovementInRepository,
  private val externalMovementRepository: OffenderExternalMovementRepository,
) {

  fun getOffenderCourtMovements(offenderNo: String): OffenderCourtMovementsResponse {
    val allMovementsOut = courtMovementOutRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
    val allMovementsIn = courtMovementInRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
    val allSchedulesOut = courtEventRepository.findAllByOffenderBooking_Offender_NomsIdAndDirectionCode_CodeIs(offenderNo, "OUT")

    val unscheduledMovementsOut = allMovementsOut.toSet() - allSchedulesOut.mapNotNull { it.courtMovementOut }.toSet()
    val unscheduledMovementsIn = allMovementsIn.toSet() - allSchedulesOut.mapNotNull { it.courtMovementIn }.toSet()

    data class Booking(val id: Long, val active: Boolean, val latest: Boolean, val prison: String)

    val bookings = (
      allSchedulesOut.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1, it.offenderBooking.location.id) } +
        allMovementsOut.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1, it.offenderBooking.location.id) } +
        allMovementsIn.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1, it.offenderBooking.location.id) }
      ).toSet()

    return OffenderCourtMovementsResponse(
      bookings.map { bk ->
        BookingCourtMovements(
          bookingId = bk.id,
          activeBooking = bk.active,
          latestBooking = bk.latest,
          courtSchedules = allSchedulesOut
            .filter { it.offenderBooking.bookingId == bk.id }
            .map { it.toResponse(it.courtMovementOut, it.courtMovementIn) },
          unscheduledCourtMovementOuts = unscheduledMovementsOut
            .filter { it.offenderBooking.bookingId == bk.id }
            .map { it.toResponse() },
          unscheduledCourtMovementIns = unscheduledMovementsIn
            .filter { it.offenderBooking.bookingId == bk.id }
            .map { it.toResponse() },
        )
      },
    )
  }

  private fun OffenderCourtMovementOut.toResponse() = BookingCourtMovementOut(
    sequence = id.sequence,
    movementDate = movementDate,
    movementTime = movementTime,
    movementReason = movementReason.id.reasonCode,
    fromPrison = fromAgency!!.id,
    toCourt = toAgency?.id,
    commentText = commentText,
    audit = toAudit(),
  )

  private fun OffenderCourtMovementIn.toResponse() = BookingCourtMovementIn(
    sequence = id.sequence,
    movementDate = movementDate,
    movementTime = movementTime,
    movementReason = movementReason.id.reasonCode,
    fromCourt = fromAgency?.id,
    toPrison = toAgency!!.id,
    commentText = commentText,
    audit = toAudit(),
  )

  private fun CourtEvent.toResponse(moveOut: OffenderCourtMovementOut?, moveIn: OffenderCourtMovementIn?) = BookingCourtScheduleOut(
    eventId = id,
    courtMovementOut = moveOut?.toResponse(),
    courtMovementIn = moveIn?.toResponse(),
    eventDate = eventDate,
    startTime = startTime,
    eventType = courtEventType.code,
    eventStatus = eventStatus.code,
    comment = commentText,
    prison = externalMovementRepository.findPrisonAt(createDatetime, offenderBooking.bookingId)?.id ?: offenderBooking.location.id,
    court = court.id,
    courtCaseId = courtCase?.id,
    audit = toAudit(),
  )
}
