package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.advances

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.MoneySupport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAdvanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Service
@Transactional
class PrisonerAdvanceService(
  private val offenderRepository: OffenderRepository,
  private val repository: OffenderAdvanceRepository,
) {
  fun getAdvance(offenderAdvanceId: Long): PrisonerAdvanceDto = repository.findByIdOrNull(offenderAdvanceId)
    ?.toDto()
    ?: throw NotFoundException("Offender advance with id $offenderAdvanceId not found")

  fun getAdvances(prisonNumber: String, activeOnly: Boolean): List<PrisonerAdvanceDto> {
    val offender = offenderRepository.findRootByNomsId(prisonNumber)
      ?: throw NotFoundException("Offender $prisonNumber not found")

    return findAdvances(offender.id, activeOnly)
  }

  fun getAdvances(rootOffenderId: Long, activeOnly: Boolean): List<PrisonerAdvanceDto> {
    offenderRepository.findByIdOrNull(rootOffenderId)
      ?: throw NotFoundException("Offender with id $rootOffenderId not found")

    return findAdvances(rootOffenderId, activeOnly)
  }

  private fun findAdvances(rootOffenderId: Long, activeOnly: Boolean): List<PrisonerAdvanceDto> = if (activeOnly) {
    repository.findActiveAdvancesByOffenderId(rootOffenderId).map { it.toDto(AdvanceStatus.ACTIVE) }
  } else {
    repository.findByOffenderId(rootOffenderId).map { it.toDto() }
  }
}

// TODO Evaluate status for the advance
fun OffenderAdvance.toDto(status: AdvanceStatus = AdvanceStatus.TODO) = PrisonerAdvanceDto(
  id = this.id,
  prisonNumber = offender.nomsId,
  caseloadId = caseloadId,
  advanceAmount = MoneySupport.poundsToPence(advanceAmount),
  advanceDate = advanceDate,
  startDate = startDate,
  repaymentAmount = MoneySupport.poundsToPence(paymentAmount),
  reference = referenceText,
  comment = commentText,
  createdBy = createUsername,
  createDatetime = createDatetime,
  informationNumber = deduction?.informationNumber!!,
  status = status,
)
