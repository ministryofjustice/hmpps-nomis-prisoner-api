package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAdvanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Service
@Transactional
class PrisonerAdvanceService(
  private val offenderRepository: OffenderRepository,
  private val offenderAdvanceRepository: OffenderAdvanceRepository,
) {
  fun getAdvances(prisonNumber: String): List<PrisonerAdvanceDto> = offenderRepository.findRootByNomsId(prisonNumber)
    ?.let {
      offenderAdvanceRepository.findByOffenderId(it.rootOffenderId!!)
        .map {
          PrisonerAdvanceDto(
            prisonNumber = prisonNumber,
            caseloadId = it.caseloadId,
            transactionType = it.transactionType,
            advanceAmount = MoneySupport.poundsToPence(it.advanceAmount),
            advanceDate = it.advanceDate,
            startDate = it.startDate,
            repaymentAmount = MoneySupport.poundsToPence(it.paymentAmount),
            reference = it.referenceText,
            comment = it.commentText,
            createdBy = it.createUsername,
            // TODO
            status = "TODO",
          )
        }
    }
    ?: throw NotFoundException("Offender $prisonNumber not found")
}
