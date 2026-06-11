package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.offender

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Service
@Transactional
class OffenderCourtMovementsService(
  private val courtEventRepository: CourtEventRepository,
  private val courtMovementOutRepository: OffenderCourtMovementOutRepository,
  private val courtMovementInRepository: OffenderCourtMovementInRepository,
  private val externalMovementRepository: OffenderExternalMovementRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
) {

  fun getOffenderCourtMovements(offenderNo: String): OffenderCourtMovementsResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }

    val allMovementsOut = courtMovementOutRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
      .filterNot { it.createUsername == "SYS" && it.auditModuleName == "MERGE" }
    val allMovementsIn = courtMovementInRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
      .filterNot { it.createUsername == "SYS" && it.auditModuleName == "MERGE" }
    val allSchedulesOut = courtEventRepository.findAllByOffenderBooking_Offender_NomsIdAndDirectionCode_CodeIs(offenderNo, "OUT")

    // When finding unscheduled movements we also have to cross reference with the schedules we loaded - just in case any are linked to a schedule without a direction (bad old NOMIS data that we are ignoring)
    val unscheduledMovementsOut = allMovementsOut.filter { it.courtScheduleOutId == null || it.courtScheduleOutId !in(allSchedulesOut.map { it.id }) }
    val unscheduledMovementsIn = allMovementsIn.filter { it.courtScheduleOutId == null || it.courtScheduleOutId !in(allSchedulesOut.map { it.id }) }

    data class Booking(val id: Long, val active: Boolean, val latest: Boolean)

    val bookings = (
      allSchedulesOut.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) } +
        allMovementsOut.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) } +
        allMovementsIn.map { Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1) }
      ).toSet()

    return OffenderCourtMovementsResponse(
      bookings.map { bk ->
        toBookingCourtMovements(
          bookingId = bk.id,
          active = bk.active,
          latest = bk.latest,
          courtSchedules = allSchedulesOut,
          allMovementsOut = allMovementsOut,
          allMovementsIn = allMovementsIn,
          unscheduledCourtMovementOuts = unscheduledMovementsOut,
          unscheduledCourtMovementIns = unscheduledMovementsIn,
        )
      },
    )
  }

  fun getBookingCourtMovements(bookingId: Long): BookingCourtMovements {
    val booking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Offender booking $bookingId not found")

    val allMovementsOut = courtMovementOutRepository.findAllByOffenderBooking_BookingId(bookingId)
      .filterNot { it.createUsername == "SYS" && it.auditModuleName == "MERGE" }
    val allMovementsIn = courtMovementInRepository.findAllByOffenderBooking_BookingId(bookingId)
      .filterNot { it.createUsername == "SYS" && it.auditModuleName == "MERGE" }
    val allSchedulesOut = courtEventRepository.findAllByOffenderBooking_BookingIdAndDirectionCode_CodeIs(bookingId, "OUT")

    // When finding unscheduled movements we also have to cross reference with the schedules we loaded - just in case any are linked to a schedule without a direction (bad old NOMIS data that we are ignoring)
    val unscheduledMovementsOut = allMovementsOut.filter { it.courtScheduleOutId == null || it.courtScheduleOutId !in(allSchedulesOut.map { it.id }) }
    val unscheduledMovementsIn = allMovementsIn.filter { it.courtScheduleOutId == null || it.courtScheduleOutId !in(allSchedulesOut.map { it.id }) }

    return toBookingCourtMovements(
      bookingId = bookingId,
      active = booking.active,
      latest = booking.bookingSequence == 1,
      courtSchedules = allSchedulesOut,
      allMovementsOut = allMovementsOut,
      allMovementsIn = allMovementsIn,
      unscheduledCourtMovementOuts = unscheduledMovementsOut,
      unscheduledCourtMovementIns = unscheduledMovementsIn,
    )
  }

  private fun OffenderCourtMovementOut.toResponse() = BookingCourtMovementOut(
    sequence = id.sequence,
    movementDate = movementDate,
    movementTime = getMovementDateAndTime(),
    movementReason = movementReason.id.reasonCode,
    fromPrison = fromAgency!!.id,
    toCourt = toAgency?.id,
    commentText = commentText,
    audit = toAudit(),
  )

  private fun OffenderCourtMovementIn.toResponse() = BookingCourtMovementIn(
    sequence = id.sequence,
    movementDate = movementDate,
    movementTime = getMovementDateAndTime(),
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

  private fun toBookingCourtMovements(
    bookingId: Long,
    active: Boolean,
    latest: Boolean,
    courtSchedules: List<CourtEvent>,
    allMovementsOut: List<OffenderCourtMovementOut>,
    allMovementsIn: List<OffenderCourtMovementIn>,
    unscheduledCourtMovementOuts: List<OffenderCourtMovementOut>,
    unscheduledCourtMovementIns: List<OffenderCourtMovementIn>,
  ) = BookingCourtMovements(
    bookingId = bookingId,
    activeBooking = active,
    latestBooking = latest,
    courtSchedules = courtSchedules
      .filter { it.offenderBooking.bookingId == bookingId }
      .map { schedule ->
        schedule.toResponse(
          moveOut = allMovementsOut.find { it.courtScheduleOutId == schedule.id && it.offenderBooking.bookingId == bookingId },
          moveIn = allMovementsIn.find { it.courtScheduleOutId == schedule.id && it.offenderBooking.bookingId == bookingId },
        )
      },
    unscheduledCourtMovementOuts = unscheduledCourtMovementOuts
      .filter { it.offenderBooking.bookingId == bookingId }
      .map { it.toResponse() },
    unscheduledCourtMovementIns = unscheduledCourtMovementIns
      .filter { it.offenderBooking.bookingId == bookingId }
      .map { it.toResponse() },
  )
}
