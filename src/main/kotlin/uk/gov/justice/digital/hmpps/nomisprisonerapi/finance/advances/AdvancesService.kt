package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.advances

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.MoneySupport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAdvanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAdvanceWithStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AdvancesService(
  private val offenderRepository: OffenderRepository,
  private val repository: OffenderAdvanceRepository,
) {
  fun getAdvance(offenderAdvanceId: Long): PrisonerAdvanceDto = repository.findByAdvanceId(offenderAdvanceId)
    ?.toDto()
    ?: throw NotFoundException("Offender advance with id $offenderAdvanceId not found")

  fun getAdvances(prisonNumber: String, activeOnly: Boolean): List<PrisonerAdvanceDto> {
    val offender = offenderRepository.findRootByNomsId(prisonNumber)
      ?: throw NotFoundException("Offender $prisonNumber not found")

    return findAdvances(offender.id, activeOnly)
  }

  fun getAdvances(rootOffenderId: Long, activeOnly: Boolean): List<PrisonerAdvanceDto> {
    if (!offenderRepository.existsById(rootOffenderId)) {
      throw NotFoundException("Offender with id $rootOffenderId not found")
    }

    return findAdvances(rootOffenderId, activeOnly)
  }

  fun findAdvances(rootOffenderId: Long, activeOnly: Boolean): List<PrisonerAdvanceDto> = if (activeOnly) {
    repository.findActiveAdvancesByOffenderId(rootOffenderId).map { it.toDto() }
  } else {
    repository.findAdvancesByOffenderId(rootOffenderId).map { it.toDto() }
  }

  fun getActiveAdvancesCount(): AdvancesCount = AdvancesCount(repository.findActiveAdvancesCount())

  fun getAdvanceByDeductionId(deductionId: Long): PrisonerAdvanceDto = repository.findByDeductionId(deductionId)
    ?.toDto()
    ?: throw NotFoundException("Offender advance with deduction id $deductionId not found")
}

fun OffenderAdvanceWithStatus.toDto() = PrisonerAdvanceDto(
  id = id,
  prisonNumber = prisonNumber,
  caseloadId = caseloadId,
  advanceAmount = MoneySupport.poundsToPence(advanceAmount),
  advanceDate = advanceDate?.toLocalDate(),
  outstandingBalance = MoneySupport.poundsToPence(
    advanceAmount -
      ((deductionAmount ?: BigDecimal.ZERO) + (adjustmentAmount ?: BigDecimal.ZERO)),
  ),
  startDate = startDate.toLocalDate(),
  repaymentAmount = MoneySupport.poundsToPence(paymentAmount),
  reference = reference,
  comment = commentText,
  informationNumber = informationNumber,
  deductionAmount = deductionAmount?.let { MoneySupport.poundsToPence(it) },
  adjustmentReason = adjustmentReason,
  adjustmentAmount = adjustmentAmount?.let { MoneySupport.poundsToPence(it) },
  status = AdvanceStatus.valueOf(status),
  advanceCreateTransactionId = advanceCreateTransactionId,
  // TODO
  lastTransactionTimestamp = LocalDateTime.parse("2025-12-20T01:02:03"),
  createdBy = createdBy,
  createDatetime = createDatetime,
)
