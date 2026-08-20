package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.offender

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.asDisplayName
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.movement.TransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.DEFAULT_TRANSFER_PRIORITY_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.TransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.TransferScheduleWaitlist

@Service
@Transactional
class OffenderTransferMovementsService(
  private val scheduleRepository: OffenderTransferScheduleOutRepository,
  private val movementRepository: OffenderTransferMovementOutRepository,
  private val offenderRepository: OffenderRepository,
) {

  fun getOffenderTransferMovements(rootOffenderId: Long): OffenderTransferMovementsResponse {
    val offender = offenderRepository.findByIdOrNull(rootOffenderId)
      ?: throw NotFoundException("Offender with id $rootOffenderId not found")

    return getOffenderTransferMovementDetails(offender)
  }

  fun getOffenderTransferMovements(offenderNo: String): OffenderTransferMovementsResponse {
    val offender = offenderRepository.findRootByNomsId(offenderNo)
      ?: throw NotFoundException("Offender with nomsId=$offenderNo not found")

    return getOffenderTransferMovementDetails(offender)
  }

  private fun getOffenderTransferMovementDetails(offender: Offender): OffenderTransferMovementsResponse {
    val offenderNo = offender.nomsId
    val allSchedules = scheduleRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
    val allMovements = movementRepository.findAllByOffenderBooking_Offender_NomsId(offenderNo)
      .filterNot { it.createUsername == "SYS" && it.auditModuleName == "MERGE" }
    val unscheduledMovements = (allSchedules.map { it.eventId }).let { allEventIds ->
      allMovements.filter { it.transferScheduleOutId == null || it.transferScheduleOutId !in (allEventIds) }
    }

    data class Booking(val id: Long, val active: Boolean, val latest: Boolean)

    val bookings = (
      allSchedules.map {
        Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1)
      } +
        allMovements.map {
          Booking(it.offenderBooking.bookingId, it.offenderBooking.active, it.offenderBooking.bookingSequence == 1)
        }
      ).toSet()

    return OffenderTransferMovementsResponse(
      offenderNo = offenderNo,
      rootOffenderId = offender.rootOffenderId!!,
      bookings.map { bk ->
        toBookingTransferMovements(
          bookingId = bk.id,
          active = bk.active,
          latest = bk.latest,
          schedules = allSchedules,
          allMovements = allMovements,
          unscheduledMovements = unscheduledMovements,
        )
      },
    )
  }

  private fun toBookingTransferMovements(
    bookingId: Long,
    active: Boolean,
    latest: Boolean,
    schedules: List<OffenderTransferScheduleOut>,
    allMovements: List<OffenderTransferMovementOut>,
    unscheduledMovements: List<OffenderTransferMovementOut>,
  ) = BookingTransferMovements(
    bookingId = bookingId,
    activeBooking = active,
    latestBooking = latest,
    transferSchedules = schedules
      .filter { it.offenderBooking.bookingId == bookingId }
      .map { schedule ->
        schedule.toResponse(
          movement = allMovements.find { it.transferScheduleOutId == schedule.eventId && it.offenderBooking.bookingId == bookingId },
        )
      },
    unscheduledTransferMovements = unscheduledMovements
      .filter { it.offenderBooking.bookingId == bookingId }
      .map { it.toResponse() },
  )

  private fun OffenderTransferScheduleOut.toResponse(movement: OffenderTransferMovementOut?) = BookingTransferSchedule(
    schedule = TransferScheduleOut(
      bookingId = offenderBooking.bookingId,
      eventId = eventId,
      startTime = this.getAppointmentStartDateAndTime(),
      eventSubType = eventSubType.id.code,
      eventStatus = eventStatus.code,
      comment = comment,
      hiddenComment = hiddenComment,
      fromPrison = fromAgency!!.id,
      toPrison = toAgency!!.id,
      cancellationReasonCode = cancellationReasonCode?.code,
      escortCode = escort?.code,
      audit = toAudit(),
      userActiveCaseloadId = null,
      waitlist = waitList?.let {
        TransferScheduleWaitlist(
          requestDate = it.requestDate,
          status = it.waitListStatus.code,
          statusDate = it.statusDate,
          priority = it.transferPriority?.code ?: DEFAULT_TRANSFER_PRIORITY_CODE,
          approved = it.approvedFlag,
          approvedUserName = it.approvedStaff?.asDisplayName(),
          cancellationReasonCode = it.cancellationReasonCode?.code,
          comment = it.commentText1,
          audit = it.toAudit(),
          userActiveCaseloadId = null,
        )
      },
    ),
    movement = movement?.toResponse(),
  )

  private fun OffenderTransferMovementOut.toResponse() = TransferMovementOut(
    bookingId = offenderBooking.bookingId,
    sequence = id.sequence,
    eventId = transferScheduleOutId,
    transferScheduleOutId = transferScheduleOutId,
    movementTime = getMovementDateAndTime(),
    movementReason = movementReason.id.reasonCode,
    escort = escort?.code,
    fromPrison = fromAgency!!.id,
    toPrison = toAgency!!.id,
    active = active,
    commentText = commentText,
    audit = toAudit(),
    userActiveCaseloadId = null,
  )
}
