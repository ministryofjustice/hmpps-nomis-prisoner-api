package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.AccountSummaryDto
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.AggregatedAccountDto
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSubAccountId
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface OffenderSubAccountRepository : JpaRepository<OffenderSubAccount, OffenderSubAccountId> {

  @Query(
    """ 
     WITH osa AS (
      SELECT
          caseload_id,
          trust_account_code,
          last_txn_id,
          balance,
          hold_balance,
          create_datetime,
          modify_datetime,
          CASE trust_account_code
              WHEN 2101 THEN 'REG'
              WHEN 2102 THEN 'SPND'
              WHEN 2103 THEN 'SAV'
          END AS sub_account_type
      FROM offender_sub_accounts
      WHERE offender_id = :offenderId
  ),
  ot AS (
      SELECT
          txn_id,
          sub_account_type,
          MAX(create_datetime) AS create_datetime
      FROM offender_transactions
      GROUP BY txn_id, sub_account_type
  ),
  gl AS (
      SELECT
          txn_id,
          MAX(txn_entry_date) AS txn_entry_date,
          MAX(txn_entry_time) AS txn_entry_time
      FROM gl_transactions
      WHERE offender_id = :offenderId
      GROUP BY txn_id
  )
  SELECT
      osa.caseload_id        AS prisonId,
      osa.trust_account_code AS accountCode,
      osa.last_txn_id        AS lastTransactionId,
      osa.balance            AS balance,
      osa.hold_balance       AS holdBalance,
      osa.create_datetime    AS createDateTime,
      osa.modify_datetime    AS modifyDateTime,
      COALESCE(gl.txn_entry_date, TRUNC(ot.create_datetime)) AS txnEntryDate,
      COALESCE(gl.txn_entry_time, ot.create_datetime)        AS txnEntryTime
  FROM osa
  JOIN ot
    ON ot.txn_id = osa.last_txn_id
   AND ot.sub_account_type = osa.sub_account_type
  LEFT JOIN gl
    ON gl.txn_id = osa.last_txn_id
  ORDER BY
      osa.caseload_id,
      osa.trust_account_code
  """,
    nativeQuery = true,
  )
  fun findByOffenderIdWithTransactionDateTime(offenderId: Long): List<OffenderSubAccounWithTransactionDateTimeProjection>

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

  @Query(
    """
        select new uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.AggregatedAccountDto(
        o.id.accountCode,
        sum(o.balance)
    )
    from OffenderSubAccount o
    where o.id.offender.id = :rootOffenderId
    group by o.id.accountCode
    order by o.id.accountCode
    """,
  )
  fun getAggregatedAccounts(rootOffenderId: Long): List<AggregatedAccountDto>
}

interface OffenderSubAccounWithTransactionDateTimeProjection {
  val prisonId: String
  val accountCode: Long
  val balance: BigDecimal
  val holdBalance: BigDecimal?
  val lastTransactionId: Long
  val createDateTime: LocalDateTime
  val txnEntryDate: LocalDateTime
  val txnEntryTime: LocalDateTime
}
