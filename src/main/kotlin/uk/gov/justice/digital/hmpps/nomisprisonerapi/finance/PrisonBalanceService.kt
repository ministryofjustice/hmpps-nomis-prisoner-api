package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadRepository

@Service
@Transactional(readOnly = true)
class PrisonBalanceService(
  private val caseloadRepository: CaseloadRepository,
) {
  fun findAllIds(): List<String> = caseloadRepository.findAllCaseloadsWithAccountBalance()
}
