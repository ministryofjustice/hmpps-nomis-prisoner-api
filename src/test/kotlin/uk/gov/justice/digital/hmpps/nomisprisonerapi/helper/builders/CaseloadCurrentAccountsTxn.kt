package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AccountCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AccountPeriod
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsTxn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadCurrentAccountsTxnRepository
import java.math.BigDecimal
import java.time.LocalDateTime

@DslMarker
annotation class CaseloadCurrentAccountsTxnDslMarker

@NomisDataDslMarker
interface CaseloadCurrentAccountsTxnDsl

@Component
class CaseloadCurrentAccountsTxnBuilderRepository(
  private val caseloadCurrentAccountsTxnRepository: CaseloadCurrentAccountsTxnRepository,
) {
  fun save(
    caseloadCurrentAccountsTxn: CaseloadCurrentAccountsTxn,
  ): CaseloadCurrentAccountsTxn = caseloadCurrentAccountsTxnRepository
    .saveAndFlush(caseloadCurrentAccountsTxn)
}

@Component
class CaseloadCurrentAccountsTxnBuilderFactory(
  val repository: CaseloadCurrentAccountsTxnBuilderRepository,
) {
  fun builder() = CaseloadCurrentAccountsTxnBuilder(repository)
}

class CaseloadCurrentAccountsTxnBuilder(
  private val repository: CaseloadCurrentAccountsTxnBuilderRepository,

) : CaseloadCurrentAccountsTxnDsl {
  fun build(
    caseloadId: String,
    accountCode: AccountCode,
    accountPeriod: AccountPeriod,
    currentBalance: BigDecimal,
    createDateTime: LocalDateTime,
  ): CaseloadCurrentAccountsTxn = CaseloadCurrentAccountsTxn(
    caseloadId = caseloadId,
    accountCode = accountCode,
    accountPeriod = accountPeriod,
    currentBalance = currentBalance,
    createDateTime = createDateTime,
  )
    .let { repository.save(it) }
}
