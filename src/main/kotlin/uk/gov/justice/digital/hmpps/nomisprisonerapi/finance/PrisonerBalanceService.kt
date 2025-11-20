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

  fun getPrisonerAccounts(rootOffenderId: Long): PrisonerBalanceDto = offenderRepository.findByIdOrNull(rootOffenderId)
    ?.let { offender ->
      PrisonerBalanceDto(
        rootOffenderId,
        prisonNumber = offender.nomsId,
        offenderSubAccountRepository.findByIdOffenderId(rootOffenderId).map { it.toPrisonerAccountDto() },
      )
    }
    ?: throw NotFoundException("Offender with id $rootOffenderId not found")

  fun findAllPrisonersWithAccountBalance(prisonIds: List<String>?, pageRequest: Pageable): PagedModel<Long> = PagedModel(
    offenderRepository.findAllOffenderIdsWithBalances(prisonIds, pageRequest),
  )

  fun findAllPrisonersWithAccountBalanceFromId(rootOffenderId: Long, pageSize: Int, prisonIds: List<String>?): PrisonerBalanceResource.RootOffenderIdsWithLast = offenderRepository.findAllOffendersIdsWithBalancesFromId(rootOffenderId, prisonIds, pageSize).let {
    PrisonerBalanceResource.RootOffenderIdsWithLast(lastOffenderId = it.lastOrNull() ?: 0, rootOffenderIds = it)
  }
}

private fun OffenderSubAccount.toPrisonerAccountDto() = PrisonerAccountDto(
  prisonId = id.caseloadId,
  lastTransactionId = lastTransactionId,
  accountCode = id.accountCode,
  balance = balance,
  holdBalance = holdBalance,
  transactionDate = modifyDatetime ?: createDatetime,
)
