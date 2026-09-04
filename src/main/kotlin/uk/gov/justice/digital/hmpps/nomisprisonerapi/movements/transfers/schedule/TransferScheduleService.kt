package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus.Companion.COMPLETED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleWaitList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers.Companion.MAX_TRANSFER_SCHEDULER_COMMENT_LENGTH
import java.time.LocalDate

internal const val DEFAULT_TRANSFER_PRIORITY_CODE = "2" // Medium - used when the DB holds an invalid/expired code

@Transactional(readOnly = true)
@Service
class TransferScheduleService(
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val transferScheduleOutRepository: OffenderTransferScheduleOutRepository,
  private val transferMovementOutRepository: OffenderTransferMovementOutRepository,
  private val movementHelpers: MovementHelpers,
) {
  fun getTransferScheduleOut(offenderNo: String, eventId: Long): TransferScheduleOut {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Offender with nomsId=$offenderNo not found")
    }
    return transferScheduleOutRepository.findByIdOrNull(eventId)
      ?.takeIf { it.offenderBooking.offender.nomsId == offenderNo }
      ?.toResponse()
      ?: throw NotFoundException("Transfer schedule out with id=$eventId not found for prisoner with nomsId=$offenderNo")
  }

  private fun OffenderTransferScheduleOut.toResponse() = TransferScheduleOut(
    bookingId = offenderBooking.bookingId,
    eventId = eventId,
    startTime = getAppointmentStartDateAndTime(),
    eventSubType = eventSubType.code,
    eventStatus = eventStatus.code,
    comment = comment,
    hiddenComment = hiddenComment,
    fromPrison = fromAgency!!.id,
    toPrison = toAgency?.id,
    cancellationReasonCode = cancellationReasonCode?.code,
    escortCode = escort?.code,
    waitlist = waitList?.toResponse(),
    audit = toAudit(),
    userActiveCaseloadId = movementHelpers.activeCaseloadId(modifyUserId ?: createUsername),
  )

  private fun OffenderTransferScheduleWaitList.toResponse() = TransferScheduleWaitlist(
    requestDate = requestDate,
    status = waitListStatus.code,
    statusDate = statusDate,
    priority = transferPriority?.code ?: DEFAULT_TRANSFER_PRIORITY_CODE,
    approved = approvedFlag,
    approvedUserName = approvedStaff?.accounts?.firstOrNull()?.username,
    cancellationReasonCode = cancellationReasonCode?.code,
    comment = commentText1,
    audit = toAudit(),
    userActiveCaseloadId = movementHelpers.activeCaseloadId(modifyUserId ?: createUsername),
  )

  @Transactional
  fun upsertTransferScheduleOut(
    offenderNo: String,
    request: UpsertTransferScheduleOut,
  ): UpsertTransferScheduleOutResponse {
    val offenderBooking = movementHelpers.offenderBookingOrThrow(offenderNo)
    val eventSubType = movementHelpers.movementReasonOrThrow(request.eventSubType)
    val eventStatus = movementHelpers.eventStatusOrThrow(request.eventStatus)
    val escort = request.escortCode?.let { movementHelpers.escortOrThrow(it) }
    val fromPrison = movementHelpers.agencyLocationOrThrow(request.fromPrison)
    val toPrison = request.toPrison?.let { movementHelpers.agencyLocationOrThrow(it) }

    val schedule = request.eventId
      ?.let { transferScheduleOutRepository.findByEventIdOrNullWaitForLock(it) }
      ?: OffenderTransferScheduleOut(
        offenderBooking = offenderBooking,
        eventSubType = eventSubType,
        eventStatus = eventStatus,
        fromPrison = fromPrison,
      )
    if (schedule.waitList == null && request.waitlist != null) {
      schedule.waitList = OffenderTransferScheduleWaitList(
        schedule = schedule,
        requestDate = request.waitlist.requestDate,
        waitListStatus = movementHelpers.transferScheduleStatusOrThrow("PEN"),
        statusDate = LocalDate.now(),
        approvedFlag = false,
        transferPriority = movementHelpers.transferPriorityOrThrow(request.waitlist.priority),
      )
    }

    schedule.apply {
      if (request.startTime != null) {
        this.setAppointmentStartDateAndTime(request.startTime.toLocalDate(), request.startTime.toLocalTime())
      } else {
        this.setAppointmentStartDateAndTimeNull()
      }
      this.eventSubType = eventSubType
      this.eventStatus = eventStatus
      this.comment = request.comment?.truncateToUtf8Length(MAX_TRANSFER_SCHEDULER_COMMENT_LENGTH, includeSeeDpsSuffix = true)
      this.fromAgency = fromPrison
      this.toAgency = toPrison
      this.escort = escort
      if (request.waitlist != null) {
        waitList!!.update(request.waitlist)
      } else {
        this.waitList = null
      }
    }

    return transferScheduleOutRepository.save(schedule)
      .let {
        UpsertTransferScheduleOutResponse(offenderBooking.bookingId, schedule.eventId)
      }
  }

  @Transactional
  fun deleteTransferScheduleOut(offenderNo: String, eventId: Long) {
    transferScheduleOutRepository.findByEventIdOrNullWaitForLock(eventId)
      ?.also { schedule ->
        transferMovementOutRepository.findByTransferScheduleOutId(eventId)
          ?.run { throw ConflictException("Cannot delete transfer schedule out eventId $eventId because it has a movement ${id.offenderBooking.bookingId} / ${id.sequence}") }

        if (schedule.eventStatus.code == COMPLETED) {
          throw ConflictException("Cannot delete transfer schedule out eventId $eventId because it has status $COMPLETED")
        }

        offenderBookingRepository.findByIdOrNull(schedule.offenderBooking.bookingId)
          ?.takeIf { it.offender.nomsId != offenderNo }
          ?.run { throw ConflictException("EventId $eventId exists on a different offender") }

        transferScheduleOutRepository.delete(schedule)
      }
  }

  private fun OffenderTransferScheduleWaitList.update(request: UpsertTransferScheduleWaitlist) {
    val requestedStatus = request.status.let { movementHelpers.transferScheduleStatusOrThrow(it) }
    val requestedPriority = request.priority.let { movementHelpers.transferPriorityOrThrow(it) }
    val requestedApprovedStaff = request.approvedUserName?.let { movementHelpers.approvedStaff(it) }

    // We only ever set to the default cancellation reason as DPS don't model cancel reason at all
    val defaultCancellationReason = movementHelpers.transferCancellationReasonOrThrow("ADMI")

    this.requestDate = request.requestDate
    this.transferPriority = requestedPriority
    this.commentText1 = request.comment?.truncateToUtf8Length(MAX_TRANSFER_SCHEDULER_COMMENT_LENGTH, includeSeeDpsSuffix = true)

    // Do nothing if status not changed
    if (requestedStatus != this.waitListStatus) {
      this.waitListStatus = requestedStatus
      this.statusDate = LocalDate.now()

      when (request.status) {
        "PEN" -> {
          this.approvedFlag = false
          this.approvedStaff = null
          this.cancellationReasonCode = null
        }

        "CON" -> {
          this.approvedFlag = true
          this.approvedStaff = requestedApprovedStaff?.staff
          this.cancellationReasonCode = null
        }

        "CAN" -> {
          this.approvedFlag = false
          this.approvedStaff = null
          this.cancellationReasonCode = defaultCancellationReason
        }

        else -> throw BadDataException("Invalid transfer waitlist status: ${requestedStatus.code}")
      }
    }
  }
}
