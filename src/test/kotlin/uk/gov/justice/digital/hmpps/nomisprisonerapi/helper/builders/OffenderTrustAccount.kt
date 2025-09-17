package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccountId
import java.math.BigDecimal

@DslMarker
annotation class OffenderTrustAccountDslMarker

@NomisDataDslMarker
interface OffenderTrustAccountDsl {

  @OffenderSubAccountDslMarker
  fun subAccount(
    accountCode: Long,
    balance: BigDecimal,
    dsl: OffenderSubAccountDsl.() -> Unit = {},
  ): OffenderSubAccount
}

@Component
class OffenderTrustAccountBuilderFactory(private val offenderSubAccountBuilderFactory: OffenderSubAccountBuilderFactory) {
  fun builder() = OffenderTrustAccountBuilder(offenderSubAccountBuilderFactory)
}

class OffenderTrustAccountBuilder(
  private val offenderSubAccountBuilderFactory: OffenderSubAccountBuilderFactory,

) : OffenderTrustAccountDsl {
  private lateinit var offenderTrustAccount: OffenderTrustAccount

  fun build(
    offender: Offender,
    caseloadId: String,
    currentBalance: BigDecimal,
    holdBalance: BigDecimal,
  ): OffenderTrustAccount = OffenderTrustAccount(
    id = OffenderTrustAccountId(caseloadId = caseloadId, offender = offender),
    currentBalance = currentBalance,
    holdBalance = holdBalance,
    accountClosed = false,
  ).also { offenderTrustAccount = it }

  override fun subAccount(
    accountCode: Long,
    balance: BigDecimal,
    dsl: OffenderSubAccountDsl.() -> Unit,
  ): OffenderSubAccount = offenderSubAccountBuilderFactory.builder().let { builder ->
    builder.build(
      offender = offenderTrustAccount.id.offender,
      caseloadId = offenderTrustAccount.id.caseloadId,
      accountCode = accountCode,
      balance = balance,
    )
  }
}
