package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AccountCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AccountPeriod
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBaseId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsTxn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AccountCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AccountPeriodRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadCurrentAccountsBaseRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@DslMarker
annotation class CaseloadCurrentAccountsBaseDslMarker

@NomisDataDslMarker
interface CaseloadCurrentAccountsBaseDsl {
  @CaseloadCurrentAccountsTxnDslMarker
  fun transaction(
    currentBalance: BigDecimal,
    createDateTime: LocalDateTime = LocalDateTime.now(),
  ): CaseloadCurrentAccountsTxn
}

@Component
class CaseloadCurrentAccountsBaseBuilderRepository(
  private val caseloadCurrentAccountsBaseRepository: CaseloadCurrentAccountsBaseRepository,
  private val accountCodeRepository: AccountCodeRepository,
  private val accountPeriodRepository: AccountPeriodRepository,
) {
  fun lookupAccountCode(code: Int): AccountCode = accountCodeRepository
    .findById(code).orElseThrow { NotFoundException("not found: accountCode=$code") }

  fun lookupAccountPeriod(period: Int): AccountPeriod = accountPeriodRepository
    .findById(period).orElseThrow { NotFoundException("not found: accountPeriod=$period") }

  fun save(
    caseloadCurrentAccountsBase: CaseloadCurrentAccountsBase,
  ): CaseloadCurrentAccountsBase = caseloadCurrentAccountsBaseRepository
    .saveAndFlush(caseloadCurrentAccountsBase)
}

@Component
class CaseloadCurrentAccountsBaseBuilderFactory(
  private val caseloadCurrentAccountsTxnBuilderFactory: CaseloadCurrentAccountsTxnBuilderFactory,
  private val repository: CaseloadCurrentAccountsBaseBuilderRepository,
) {
  fun builder() = CaseloadCurrentAccountsBaseBuilder(caseloadCurrentAccountsTxnBuilderFactory, repository)
}

class CaseloadCurrentAccountsBaseBuilder(
  private val caseloadCurrentAccountsTxnBuilderFactory: CaseloadCurrentAccountsTxnBuilderFactory,
  private val repository: CaseloadCurrentAccountsBaseBuilderRepository,
) : CaseloadCurrentAccountsBaseDsl {
  private lateinit var caseloadCurrentAccountsBase: CaseloadCurrentAccountsBase
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
  ).let {
    repository.save(it)
  }.also { caseloadCurrentAccountsBase = it }

  override fun transaction(
    currentBalance: BigDecimal,
    createDateTime: LocalDateTime,
  ): CaseloadCurrentAccountsTxn = caseloadCurrentAccountsTxnBuilderFactory.builder().build(
    caseloadId = caseloadCurrentAccountsBase.id.caseloadId,
    accountCode = caseloadCurrentAccountsBase.accountCode,
    accountPeriod = caseloadCurrentAccountsBase.accountPeriod,
    currentBalance = currentBalance,
    createDateTime = createDateTime,
  )
}
