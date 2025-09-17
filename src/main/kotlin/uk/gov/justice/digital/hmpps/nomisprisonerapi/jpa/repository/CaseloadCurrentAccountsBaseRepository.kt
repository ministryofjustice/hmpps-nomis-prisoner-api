package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.PrisonAccountBalanceDto
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBaseId

@Repository
interface CaseloadCurrentAccountsBaseRepository : JpaRepository<CaseloadCurrentAccountsBase, CaseloadCurrentAccountsBaseId> {
  @Query(
    """
        SELECT
            ccab.id.accountCode,
            SUM(ccat.currentBalance),
            MAX(ccat.createDateTime)
        FROM CaseloadCurrentAccountsBase ccab
        JOIN CaseloadCurrentAccountsTxn ccat ON ccab.id.caseloadId = ccat.caseloadId
                                             AND ccab.accountCode = ccat.accountCode
        WHERE ccab.id.caseloadId = :caseloadId
        GROUP BY ccab.accountCode
  """,
  )
  fun getPrisonBalances(@Param("caseloadId") caseloadId: String): List<PrisonAccountBalanceDto>
}
