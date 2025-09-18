package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.math.BigDecimal

@Service
@Transactional
class PrisonerBalanceService(
  val offenderRepository: OffenderRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerAccounts(rootOffenderId: Long): PrisonerAccountsDto = dummyAccounts

  fun findAllPrisonersWithAccountBalance(pageRequest: Pageable): PagedModel<Long> = PagedModel(offenderRepository.findAllOffenderIdWithBalances(pageRequest))
}

val dummyAccounts = PrisonerAccountsDto(
  rootOffenderId = 12345,
  accounts = listOf(
    PrisonerAccountDto(
      prisonId = "MDI",
      lastTransactionId = 56789,
      subAccountType = SubAccountType.CASH,
      balance = BigDecimal(12.50),
    ),
    (
      PrisonerAccountDto(
        prisonId = "MDI",
        lastTransactionId = 56789,
        subAccountType = SubAccountType.SPEND,
        balance = BigDecimal(12.50),
      )
      ),
    (
      PrisonerAccountDto(
        prisonId = "MDI",
        lastTransactionId = 56789,
        subAccountType = SubAccountType.SAVINGS,
        balance = BigDecimal(12.50),
        holdBalance = BigDecimal(2.50),
      )
      ),
  ),
)
