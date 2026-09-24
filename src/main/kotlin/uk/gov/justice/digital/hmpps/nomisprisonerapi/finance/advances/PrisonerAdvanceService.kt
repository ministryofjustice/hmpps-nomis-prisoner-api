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
  private val repository: OffenderAdvanceRepository,
) {
  fun getAdvance(offenderAdvanceId: Long): PrisonerAdvanceDto = repository.findByIdOrNull(offenderAdvanceId)
    ?.toDto()
    ?: throw NotFoundException("Offender advance with id $offenderAdvanceId not found")

  fun getAdvances(prisonNumber: String): List<PrisonerAdvanceDto> = offenderRepository.findRootByNomsId(prisonNumber)
    ?.let { getAdvances(it.rootOffenderId!!) }
    ?: throw NotFoundException("Offender $prisonNumber not found")

  fun getAdvances(rootOffenderId: Long): List<PrisonerAdvanceDto> {
    offenderRepository.findByIdOrNull(rootOffenderId)
      ?: throw NotFoundException("Offender with id $rootOffenderId not found")

    return repository.findByOffenderId(rootOffenderId).map { it.toDto() }
  }
}

fun OffenderAdvance.toDto() = PrisonerAdvanceDto(
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
  // TODO
  status = "TODO",
)
