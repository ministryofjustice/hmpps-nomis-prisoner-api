package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.AccountSummaryDto
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccountId

@Repository
interface OffenderSubAccountRepository : JpaRepository<OffenderSubAccount, OffenderSubAccountId> {
  fun findByIdOffenderId(offenderId: Long): List<OffenderSubAccount>

  @Query(
    """
    from OffenderSubAccount
    where id.offender.id = :offenderId
      and (balance != 0 or holdBalance != 0)
    """,
  )
  fun findNonZeroBalances(offenderId: Long): List<OffenderSubAccount>

  @Query(
    """
        select new uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.AccountSummaryDto(
        o.id.caseloadId,
        o.id.accountCode,
        sum(o.balance)
    )
    from OffenderSubAccount o
    where o.id.offender.id = :offenderId
      and o.balance != 0
    group by o.id.caseloadId, o.id.accountCode
    order by o.id.caseloadId, o.id.accountCode
    """,
  )
  fun findOffenderSubAccountSummary(offenderId: Long): List<AccountSummaryDto>
}
