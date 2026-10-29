package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.scheduledpayments

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.MoneySupport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledPaymentRepository
import kotlin.collections.map

@Service
@Transactional(readOnly = true)
class ScheduledPaymentsService(
  private val offenderRepository: OffenderRepository,
  private val repository: OffenderScheduledPaymentRepository,
) {
  fun getScheduledPayment(scheduledPaymentId: Long): BasicScheduledPaymentDto = repository.findByIdOrNull(scheduledPaymentId)
    ?.toDto()
    ?: throw NotFoundException("Offender scheduled payment with id $scheduledPaymentId not found")

  fun getScheduledPayments(prisonNumber: String, activeOnly: Boolean): List<ScheduledPaymentDto> {
    val offender = offenderRepository.findRootByNomsId(prisonNumber)
      ?: throw NotFoundException("Offender $prisonNumber not found")

    return findScheduledPayments(offender.id, activeOnly)
  }

  fun getScheduledPayments(rootOffenderId: Long, activeOnly: Boolean): List<ScheduledPaymentDto> {
    if (!offenderRepository.existsById(rootOffenderId)) {
      throw NotFoundException("Offender with id $rootOffenderId not found")
    }

    return findScheduledPayments(rootOffenderId, activeOnly)
  }

  fun findScheduledPayments(rootOffenderId: Long, activeOnly: Boolean): List<ScheduledPaymentDto> = repository.findScheduledPayments(rootOffenderId, activeOnly).map { it.toFullDto() }

  fun getActiveScheduledPaymentsCount(): ScheduledPaymentsCount = ScheduledPaymentsCount(repository.getActiveScheduledPaymentsCount())
}

fun OffenderScheduledPayment.toDto() = BasicScheduledPaymentDto(
  id = this.id,
  prisonNumber = offender.nomsId,
  caseloadId = caseloadId,
  transactionType = transactionType,
  startDate = startDate,
  endDate = endDate,
  amount = MoneySupport.poundsToPence(paymentAmount),
  reference = referenceText,
  comment = commentText,
  createdBy = createUsername,
)

fun OffenderScheduledPayment.toFullDto() = ScheduledPaymentDto(
  id = this.id,
  prisonNumber = offender.nomsId,
  caseloadId = caseloadId,
  transactionType = transactionType,
  startDate = startDate,
  endDate = endDate,
  // TODO move to ScheduledPaymentProjection
  subAccountCode = 2102,
  amount = MoneySupport.poundsToPence(paymentAmount),
  // TODO move to ScheduledPaymentProjection
  dayOfWeek = WeekDay.MON,
  reference = referenceText,
  comment = commentText,
  createdBy = createUsername,
)
