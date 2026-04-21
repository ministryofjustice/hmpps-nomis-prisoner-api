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
        select new uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.AccountSummaryDto(
        o.id.accountCode,
        sum(o.balance),
        sum(o.holdBalance)
    )
    from OffenderSubAccount o
    where o.id.offender.id = :offenderId
    group by o.id.accountCode
    """,
  )
  fun findOffenderSubAccountSummary(offenderId: Long): List<AccountSummaryDto>
}
