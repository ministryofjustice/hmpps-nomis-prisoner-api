package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleWaitList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository

private const val DEFAULT_TRANSFER_PRIORITY_CODE = "2" // Medium - used when the DB holds an invalid/expired code

@Transactional(readOnly = true)
@Service
class TransferScheduleService(
  private val offenderRepository: OffenderRepository,
  private val transferScheduleOutRepository: OffenderTransferScheduleOutRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
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
    userActiveCaseloadId = activeCaseloadId(modifyUserId ?: createUsername),
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
    userActiveCaseloadId = activeCaseloadId(modifyUserId ?: createUsername),
  )

  private fun activeCaseloadId(username: String) = staffUserAccountRepository.findByUsername(username)?.activeCaseloadId
}
