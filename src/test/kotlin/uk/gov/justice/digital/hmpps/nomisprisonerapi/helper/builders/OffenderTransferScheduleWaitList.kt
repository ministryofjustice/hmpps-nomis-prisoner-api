package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleWaitList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TransferCancellationReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TransferPriority
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TransferScheduleStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIndividualScheduleWaitListRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class OffenderTransferScheduleWaitListDslMarker

@OffenderTransferScheduleWaitListDslMarker
interface OffenderTransferScheduleWaitListDsl

@Component
class OffenderTransferScheduleWaitListBuilderRepository(
  private val repository: OffenderIndividualScheduleWaitListRepository,
  private val waitListStatusRepository: ReferenceCodeRepository<TransferScheduleStatus>,
  private val transferPriorityRepository: ReferenceCodeRepository<TransferPriority>,
  private val cancellationReasonRepository: ReferenceCodeRepository<TransferCancellationReason>,
) {
  fun save(waitList: OffenderTransferScheduleWaitList): OffenderTransferScheduleWaitList = repository.saveAndFlush(waitList)
  fun waitListStatusOf(code: String): TransferScheduleStatus = waitListStatusRepository.findByIdOrNull(TransferScheduleStatus.pk(code))!!
  fun transferPriorityOf(code: String): TransferPriority = transferPriorityRepository.findByIdOrNull(TransferPriority.pk(code))!!
  fun cancellationReasonOf(code: String): TransferCancellationReason = cancellationReasonRepository.findByIdOrNull(TransferCancellationReason.pk(code))!!
}

@Component
class OffenderTransferScheduleWaitListBuilderFactory(
  private val repository: OffenderTransferScheduleWaitListBuilderRepository,
) {
  fun builder() = OffenderTransferScheduleWaitListBuilder(repository)
}

class OffenderTransferScheduleWaitListBuilder(
  private val repository: OffenderTransferScheduleWaitListBuilderRepository,
) : OffenderTransferScheduleWaitListDsl {

  fun build(
    schedule: OffenderTransferScheduleOut,
    requestDate: LocalDate,
    waitListStatus: String,
    statusDate: LocalDate,
    transferPriority: String,
    approvedFlag: Boolean,
    approvedStaff: Staff?,
    cancellationReasonCode: String?,
    commentText1: String?,
    commentText2: String?,
  ): OffenderTransferScheduleWaitList = OffenderTransferScheduleWaitList(
    id = schedule.eventId,
    schedule = schedule,
    requestDate = requestDate,
    waitListStatus = repository.waitListStatusOf(waitListStatus),
    statusDate = statusDate,
    transferPriority = repository.transferPriorityOf(transferPriority),
    approvedFlag = approvedFlag,
    approvedStaff = approvedStaff,
    cancellationReasonCode = cancellationReasonCode?.let { repository.cancellationReasonOf(it) },
    commentText1 = commentText1,
    commentText2 = commentText2,
  )
}
