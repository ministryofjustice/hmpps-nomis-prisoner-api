package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSubAccounWithTransactionDateTimeProjection
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSubAccountRepository

@Service
@Transactional
class PrisonerBalanceService(
  val offenderRepository: OffenderRepository,
  val offenderSubAccountRepository: OffenderSubAccountRepository,
) {
  fun getAggregatedAccounts(prisonNumber: String): PrisonerAggregatedAccountsDto = offenderRepository.findRootByNomsId(prisonNumber)?.let {
    getAggregatedAccounts(it.rootOffenderId!!)
  }
    ?: throw NotFoundException("Offender $prisonNumber not found")

  fun getAggregatedAccounts(rootOffenderId: Long): PrisonerAggregatedAccountsDto {
    val offender = offenderRepository.findByIdOrNull(rootOffenderId)
      ?: throw NotFoundException("Offender with id $rootOffenderId not found")
    val agregatedAccounts = offenderSubAccountRepository.getAggregatedAccounts(rootOffenderId)

    return PrisonerAggregatedAccountsDto(
      rootOffenderId = offender.rootOffenderId!!,
      prisonNumber = offender.nomsId,
      accounts = agregatedAccounts,
    )
  }

  fun getPrisonerAccountsWithTransactionTimestamp(rootOffenderId: Long): PrisonerBalanceDto {
    val offender = offenderRepository.findByIdOrNull(rootOffenderId)
      ?: throw NotFoundException("Offender $rootOffenderId not found")

    val accounts = offenderSubAccountRepository.findByOffenderIdWithTransactionDateTime(rootOffenderId)
    return PrisonerBalanceDto(
      rootOffenderId = rootOffenderId,
      prisonNumber = offender.nomsId,
      accounts = accounts.map { it.toPrisonerAccountDto() },
    )
  }

  fun getPrisonerAccounts(prisonNumber: String, excludeZeroBalances: Boolean): PrisonerBalanceDto = offenderRepository.findRootByNomsId(prisonNumber)
    ?.let { getPrisonerAccounts(it.rootOffenderId!!, excludeZeroBalances) }
    ?: throw NotFoundException("Offender $prisonNumber not found")

  fun getPrisonerAccounts(rootOffenderId: Long, excludeZeroBalances: Boolean): PrisonerBalanceDto {
    val offender = offenderRepository.findByIdOrNull(rootOffenderId)
      ?: throw NotFoundException("Offender with id $rootOffenderId not found")

    val accounts = if (excludeZeroBalances) {
      offenderSubAccountRepository.findNonZeroBalances(rootOffenderId)
    } else {
      offenderSubAccountRepository.findByIdOffenderId(rootOffenderId)
    }

    return PrisonerBalanceDto(
      rootOffenderId = rootOffenderId,
      prisonNumber = offender.nomsId,
      accounts = accounts.map { it.toPrisonerAccountDto() },
    )
  }

  fun getPrisonerAccountSummary(rootOffenderId: Long): PrisonerBalanceSummaryDto = offenderRepository.findByIdOrNull(rootOffenderId)
    ?.let { offender ->
      PrisonerBalanceSummaryDto(
        rootOffenderId,
        prisonNumber = offender.nomsId,
        accounts = offenderSubAccountRepository.findOffenderSubAccountSummary(rootOffenderId),
      )
    }
    ?: throw NotFoundException("Offender with id $rootOffenderId not found")

  fun findAllPrisonersWithAccountBalance(prisonIds: List<String>?, pageRequest: Pageable): PagedModel<Long> = PagedModel(
    offenderRepository.findAllOffenderIdsWithBalances(prisonIds, pageRequest),
  )

  fun findAllPrisonersWithAccountBalanceFromId(
    rootOffenderId: Long,
    pageSize: Int,
    prisonIds: List<String>?,
  ): PrisonerBalanceResource.RootOffenderIdsWithLast = (
    prisonIds
      ?.let { offenderRepository.findAllOffendersIdsWithBalancesFromId(rootOffenderId, it, pageSize) }
      ?: offenderRepository.findAllOffendersIdsWithBalancesFromId(rootOffenderId, pageSize)
    )
    .let { offenderIds ->
      PrisonerBalanceResource.RootOffenderIdsWithLast(
        lastOffenderId = offenderIds.lastOrNull() ?: 0,
        rootOffenderIds = offenderIds,
      )
    }
}

private fun OffenderSubAccounWithTransactionDateTimeProjection.toPrisonerAccountDto() = PrisonerAccountDto(
  prisonId = prisonId,
  lastTransactionId = lastTransactionId,
  accountCode = accountCode,
  balance = balance,
  holdBalance = holdBalance,
  transactionDate = txnEntryDate.toLocalDate().atTime(txnEntryTime.toLocalTime()),
)

private fun OffenderSubAccount.toPrisonerAccountDto() = PrisonerAccountDto(
  prisonId = id.caseloadId,
  lastTransactionId = lastTransactionId,
  accountCode = id.accountCode,
  balance = balance,
  holdBalance = holdBalance,
  transactionDate = modifyDatetime ?: createDatetime,
)
