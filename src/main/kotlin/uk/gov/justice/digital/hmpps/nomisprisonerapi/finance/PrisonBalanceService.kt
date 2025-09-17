package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadCurrentAccountsBaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PrisonBalanceService(
  private val caseloadRepository: CaseloadRepository,
  private val caseloadCurrentAccountsBaseRepository: CaseloadCurrentAccountsBaseRepository,
) {
  fun findAllIds(): List<String> = caseloadRepository.findAllCaseloadsWithAccountBalance()
  fun getPrisonBalance(prisonId: String): PrisonBalanceDto = PrisonBalanceDto(
    prisonId = prisonId,
    accountBalances = listOf(
      PrisonAccountBalanceDto(
        accountCode = 2101,
        balance = BigDecimal("33.12"),
        transactionDate = LocalDateTime.now(),
      ),
    ),
  )
}
