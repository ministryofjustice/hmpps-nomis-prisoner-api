package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccountId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTrustAccountRepository
import java.math.BigDecimal

@DslMarker
annotation class OffenderTrustAccountDslMarker

@NomisDataDslMarker
interface OffenderTrustAccountDsl {

  @OffenderSubAccountDslMarker
  fun subAccount(
    accountCode: Long,
    balance: BigDecimal = BigDecimal.ZERO,
    holdBalance: BigDecimal? = null,
    lastTransactionId: Long,
    dsl: OffenderSubAccountDsl.() -> Unit = {},
  ): OffenderSubAccount
}

@Component
class OffenderTrustAccountBuilderRepository(
  val repository: OffenderTrustAccountRepository,
) {
  fun lookup(offender: Offender, caseloadId: String): OffenderTrustAccount = repository.findByIdOrNull(OffenderTrustAccountId(caseloadId = caseloadId, offender = offender))
    ?: throw IllegalArgumentException("No OffenderTrustAccount found for offenderId=${offender.id} and caseloadId=$caseloadId")

  fun save(offenderTrustAccount: OffenderTrustAccount): OffenderTrustAccount = repository.saveAndFlush(offenderTrustAccount)
}

@Component
class OffenderTrustAccountBuilderFactory(
  private val repository: OffenderTrustAccountBuilderRepository,
  private val offenderSubAccountBuilderFactory: OffenderSubAccountBuilderFactory,
) {
  fun builder() = OffenderTrustAccountBuilder(repository, offenderSubAccountBuilderFactory)
}

class OffenderTrustAccountBuilder(
  private val repository: OffenderTrustAccountBuilderRepository,
  private val offenderSubAccountBuilderFactory: OffenderSubAccountBuilderFactory,
) : OffenderTrustAccountDsl {

  private lateinit var offenderTrustAccount: OffenderTrustAccount

  fun build(
    offender: Offender,
    caseloadId: String,
    currentBalance: BigDecimal,
    holdBalance: BigDecimal?,
  ): OffenderTrustAccount = OffenderTrustAccount(
    id = OffenderTrustAccountId(caseloadId = caseloadId, offender = offender),
    currentBalance = currentBalance,
    holdBalance = holdBalance,
    accountClosed = false,
  )
    .let { repository.save(it) }
    .also {
      offenderTrustAccount = it
    }

  override fun subAccount(
    accountCode: Long,
    balance: BigDecimal,
    holdBalance: BigDecimal?,
    lastTransactionId: Long,
    dsl: OffenderSubAccountDsl.() -> Unit,
  ): OffenderSubAccount = offenderSubAccountBuilderFactory.builder().let { builder ->
    builder.build(
      offender = offenderTrustAccount.id.offender,
      caseloadId = offenderTrustAccount.id.caseloadId,
      accountCode = accountCode,
      balance = balance,
      holdBalance = holdBalance,
      lastTransactionId = lastTransactionId,
    )
      .also { offenderTrustAccount.subAccounts += it }
      .also { builder.apply(dsl) }
  }
}
