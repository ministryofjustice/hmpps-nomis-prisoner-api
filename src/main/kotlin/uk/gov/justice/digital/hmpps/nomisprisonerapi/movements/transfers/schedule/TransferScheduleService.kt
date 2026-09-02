package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleWaitList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers.Companion.MAX_TRANSFER_SCHEDULER_COMMENT_LENGTH

internal const val DEFAULT_TRANSFER_PRIORITY_CODE = "2" // Medium - used when the DB holds an invalid/expired code

@Transactional(readOnly = true)
@Service
class TransferScheduleService(
  private val offenderRepository: OffenderRepository,
  private val transferScheduleOutRepository: OffenderTransferScheduleOutRepository,
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
    val waitlistStatus = request.waitlist?.status?.let { movementHelpers.transferScheduleStatusOrThrow(it) }
    val waitlistTransferPriority = request.waitlist?.priority?.let { movementHelpers.transferPriorityOrThrow(it) }
    val waitlistCancellationReason = request.waitlist?.cancellationReasonCode?.let { movementHelpers.transferCancellationReasonOrThrow(it) }
    val waitlistApprovedStaff = request.waitlist?.approvedUserName?.let { movementHelpers.approvedStaff(it) }

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
        waitListStatus = waitlistStatus ?: movementHelpers.transferScheduleStatusOrThrow("PEN"),
        statusDate = request.waitlist.statusDate,
        approvedFlag = request.waitlist.approved,
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
        with(waitList!!) {
          requestDate = request.waitlist.requestDate
          waitListStatus = waitlistStatus!!
          statusDate = request.waitlist.statusDate
          transferPriority = waitlistTransferPriority
          approvedFlag = request.waitlist.approved
          approvedStaff = waitlistApprovedStaff?.staff
          cancellationReasonCode = waitlistCancellationReason
          commentText1 = request.waitlist.comment?.truncateToUtf8Length(MAX_TRANSFER_SCHEDULER_COMMENT_LENGTH, includeSeeDpsSuffix = true)
        }
      } else {
        this.waitList = null
      }
    }

    return transferScheduleOutRepository.save(schedule)
      .let {
        UpsertTransferScheduleOutResponse(offenderBooking.bookingId, schedule.eventId)
      }
  }
}
