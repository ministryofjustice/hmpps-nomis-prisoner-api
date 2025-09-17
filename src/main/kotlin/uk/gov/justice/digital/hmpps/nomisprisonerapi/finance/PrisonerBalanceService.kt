package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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

  fun getPrisonerAccounts(prisonerId: Long): PrisonerAccountsDto = dummyAccounts

  fun findAllPrisonersWithAccountBalance(pageRequest: Pageable): Page<Long> = PageImpl(listOf(12345, 67890))
  //  offenderRepository.findAllWithBalances(pageRequest).map { PrisonerId(offenderNo = it.getNomsId()) }
}

val dummyAccounts = PrisonerAccountsDto(
  prisonerId = 12345,
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
