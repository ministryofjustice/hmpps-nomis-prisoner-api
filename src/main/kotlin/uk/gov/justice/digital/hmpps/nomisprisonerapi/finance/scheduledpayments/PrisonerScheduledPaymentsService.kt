package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledPaymentRepository
import kotlin.collections.map

@Service
@Transactional
class PrisonerScheduledPaymentsService(
  private val offenderRepository: OffenderRepository,
  private val offenderScheduledPaymentRepository: OffenderScheduledPaymentRepository,
) {
  fun getScheduledPayment(scheduledPaymentId: Long): PrisonerScheduledPaymentDto = offenderScheduledPaymentRepository.findByIdOrNull(scheduledPaymentId)
    ?.toDto()
    ?: throw NotFoundException("Offender scheduled payment with id $scheduledPaymentId not found")

  fun getScheduledPayments(prisonNumber: String): List<PrisonerScheduledPaymentDto> = offenderRepository.findRootByNomsId(prisonNumber)
    ?.let {
      offenderScheduledPaymentRepository.findByOffenderId(it.rootOffenderId!!)
        .map { it.toDto() }
    }
    ?: throw NotFoundException("Offender $prisonNumber not found")
}

fun OffenderScheduledPayment.toDto() = PrisonerScheduledPaymentDto(
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
  // TODO
  status = "TODO",
)
