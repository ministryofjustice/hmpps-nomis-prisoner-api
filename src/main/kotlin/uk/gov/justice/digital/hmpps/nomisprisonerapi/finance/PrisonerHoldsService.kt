package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonerHoldProjection

@Service
@Transactional
class PrisonerHoldsService(
  private val offenderRepository: OffenderRepository,
  private val offenderTransactionRepository: OffenderTransactionRepository,
) {
  fun getPrisonerHolds(prisonNumber: String): List<PrisonerHoldProjection> {
    val offender = offenderRepository.findRootByNomsId(prisonNumber) ?: throw NotFoundException("Offender with id $prisonNumber not found")
    return offenderTransactionRepository.getHoldTransactions(offender.id)
  }

  fun getPrisonerHolds(rootOffenderId: Long): List<PrisonerHoldProjection> {
    offenderRepository.findByIdOrNull(rootOffenderId)
      ?: throw NotFoundException("Offender with id $rootOffenderId not found")
    return offenderTransactionRepository.getHoldTransactions(rootOffenderId)
  }
}
