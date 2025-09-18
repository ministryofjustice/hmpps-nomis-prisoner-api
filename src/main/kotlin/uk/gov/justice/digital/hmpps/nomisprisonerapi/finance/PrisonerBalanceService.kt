package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSubAccountRepository

@Service
@Transactional
class PrisonerBalanceService(
  val offenderRepository: OffenderRepository,
  val offenderSubAccountRepository: OffenderSubAccountRepository,
) {

  fun getPrisonerAccounts(rootOffenderId: Long): PrisonerAccountsDto = offenderRepository.findByIdOrNull(rootOffenderId)
    ?.let { offender ->
      PrisonerAccountsDto(
        rootOffenderId,
        prisonNumber = offender.nomsId,
        offenderSubAccountRepository.findByIdOffenderId(rootOffenderId).map { it.toPrisonerAccountDto() },
      )
    }
    ?: throw NotFoundException("Offender with id $rootOffenderId not found")

  fun findAllPrisonersWithAccountBalance(pageRequest: Pageable): PagedModel<Long> = PagedModel(offenderRepository.findAllOffenderIdWithBalances(pageRequest))
}

private fun OffenderSubAccount.toPrisonerAccountDto() = PrisonerAccountDto(
  prisonId = id.caseloadId,
  lastTransactionId = lastTransactionId,
  subAccountType = when (id.accountCode) {
    2101L -> {
      SubAccountType.CASH
    }
    2102L -> {
      SubAccountType.SPEND
    }
    else -> {
      SubAccountType.SAVINGS
    }
  },
  balance = balance,
  holdBalance = holdBalance,
)
