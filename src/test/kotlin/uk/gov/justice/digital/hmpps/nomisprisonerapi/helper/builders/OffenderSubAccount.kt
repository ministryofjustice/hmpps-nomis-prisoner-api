package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccountId
import java.math.BigDecimal

@DslMarker
annotation class OffenderSubAccountDslMarker

@NomisDataDslMarker
interface OffenderSubAccountDsl

@Component
class OffenderSubAccountBuilderFactory {
  fun builder() = OffenderSubAccountBuilder()
}
class OffenderSubAccountBuilder : OffenderSubAccountDsl {

  fun build(
    offender: Offender,
    caseloadId: String,
    accountCode: Long,
    balance: BigDecimal,
  ): OffenderSubAccount = OffenderSubAccount(
    id = OffenderSubAccountId(caseloadId = caseloadId, offender = offender, accountCode = accountCode),
    balance = balance,
    holdBalance = BigDecimal(0),
    lastTransactionId = 0,
  )
}
