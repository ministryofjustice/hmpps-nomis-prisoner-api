package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AccountCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AccountPeriod
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBaseId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AccountCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AccountPeriodRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadCurrentAccountsBaseRepository
import java.math.BigDecimal

@DslMarker
annotation class CaseloadCurrentAccountsBaseDslMarker

@NomisDataDslMarker
interface CaseloadCurrentAccountsBaseDsl

@Component
class CaseloadCurrentAccountsBaseBuilderRepository(
  private val CaseloadCurrentAccountsBaseRepository: CaseloadCurrentAccountsBaseRepository,
  private val accountCodeRepository: AccountCodeRepository,
  private val accountPeriodRepository: AccountPeriodRepository,
) {
  fun lookupAccountCode(code: Int): AccountCode = accountCodeRepository
    .findById(code).orElseThrow { NotFoundException("not found: accountCode=$code") }

  fun lookupAccountPeriod(period: Int): AccountPeriod = accountPeriodRepository
    .findById(period).orElseThrow { NotFoundException("not found: accountPeriod=$period") }

  fun save(
    CaseloadCurrentAccountsBase: CaseloadCurrentAccountsBase,
  ): CaseloadCurrentAccountsBase = CaseloadCurrentAccountsBaseRepository
    .saveAndFlush(CaseloadCurrentAccountsBase)
}

@Component
class CaseloadCurrentAccountsBaseBuilderFactory(
  val repository: CaseloadCurrentAccountsBaseBuilderRepository,
) {
  fun builder() = CaseloadCurrentAccountsBaseBuilder(repository)
}

class CaseloadCurrentAccountsBaseBuilder(
  private val repository: CaseloadCurrentAccountsBaseBuilderRepository,

) : CaseloadCurrentAccountsBaseDsl {
  fun build(
    caseloadId: String,
    accountCode: Int,
    accountPeriod: Int,
    currentBalance: BigDecimal,
  ): CaseloadCurrentAccountsBase = CaseloadCurrentAccountsBase(
    id = CaseloadCurrentAccountsBaseId(caseloadId, accountCode),
    accountCode = repository.lookupAccountCode(accountCode),
    accountPeriod = repository.lookupAccountPeriod(accountPeriod),
    currentBalance = currentBalance,
  )
    .let { repository.save(it) }
}
