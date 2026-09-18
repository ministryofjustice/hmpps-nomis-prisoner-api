package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAdvanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Service
@Transactional
class PrisonerAdvanceService(
  private val offenderRepository: OffenderRepository,
  private val offenderAdvanceRepository: OffenderAdvanceRepository,
) {
  fun getAdvance(offenderAdvanceId: Long): PrisonerAdvanceDto = offenderAdvanceRepository.findByIdOrNull(offenderAdvanceId)
    ?.toDto()
    ?: throw NotFoundException("Offender advance with id $offenderAdvanceId not found")

  fun getAdvances(prisonNumber: String): List<PrisonerAdvanceDto> = offenderRepository.findRootByNomsId(prisonNumber)
    ?.let {
      offenderAdvanceRepository.findByOffenderId(it.rootOffenderId!!)
        .map { it.toDto() }
    }
    ?: throw NotFoundException("Offender $prisonNumber not found")
}

fun OffenderAdvance.toDto() = PrisonerAdvanceDto(
  id = this.id,
  prisonNumber = offender.nomsId,
  caseloadId = caseloadId,
  transactionType = transactionType,
  advanceAmount = MoneySupport.poundsToPence(advanceAmount),
  advanceDate = advanceDate,
  startDate = startDate,
  repaymentAmount = MoneySupport.poundsToPence(paymentAmount),
  reference = referenceText,
  comment = commentText,
  createdBy = createUsername,
  // TODO
  status = "TODO",
)
