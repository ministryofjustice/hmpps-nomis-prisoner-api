package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import java.math.BigDecimal
import java.time.LocalDateTime

private const val ADVANCE_STATUS_SELECT = """
  SELECT
    opp.offender_payment_profile_id AS id,
    o.offender_id_display AS prisonNumber,
    opp.caseload_id AS caseloadId,
    opp.advance_amount AS advanceAmount,
    opp.advance_date AS advanceDate,
    opp.start_date AS startDate,
    opp.payment_amount AS paymentAmount,
    opp.reference_text AS reference,
    opp.comment_text AS commentText,
    opp.create_user_id AS createdBy,
    opp.create_datetime AS createDatetime,
    od.information_number AS informationNumber,
    od.deduction_amount AS deductionAmount,
    od.adjustment_reason_code AS adjustmentReason,
    od.adjustment_amount AS adjustmentAmount,
    ot.txn_id AS advanceCreateTransactionId,
    CASE
      WHEN od.adjustment_reason_code IN ('WRT', 'P')
        THEN 'WRITTEN_OFF'
      WHEN od.max_total_amount = NVL(od.deduction_amount, 0)
        AND NVL(od.adjustment_amount, 0) = 0
        THEN 'REPAID'
      WHEN od.max_total_amount >
        NVL(od.deduction_amount, 0) + NVL(od.adjustment_amount, 0)
        THEN 'ACTIVE'
      ELSE 'OTHER'
    END AS status
  FROM offender_payment_profiles opp
  JOIN offender_deductions od ON opp.offender_payment_profile_id = od.offender_payment_profile_id
  JOIN offenders o ON opp.offender_id = o.offender_id
  JOIN offender_transactions ot ON ot.offender_id = opp.offender_id
    AND ot.txn_type = 'ADV'
    AND ot.txn_posting_type = 'CR'
    AND ot.txn_entry_amount = opp.advance_amount
    AND ot.txn_entry_date = opp.advance_date"""

private const val ADVANCE_BASE_CONDITIONS = """
  opp.payment_amount > 0
  AND od.deduction_status = 'A'
"""

private const val ACTIVE_ADVANCE_CONDITIONS = """
  $ADVANCE_BASE_CONDITIONS
  AND od.max_total_amount >
    NVL(od.deduction_amount, 0) + NVL(od.adjustment_amount, 0)
  AND (
    od.adjustment_reason_code IS NULL
    OR od.adjustment_reason_code NOT IN ('WRT', 'P')
  )
"""

@Repository
interface OffenderAdvanceRepository : JpaRepository<OffenderAdvance, Long> {

  @Query(
    ADVANCE_STATUS_SELECT +
      """
      WHERE opp.offender_payment_profile_id = :advanceId
        AND $ADVANCE_BASE_CONDITIONS
    """,
    nativeQuery = true,
  )
  fun findByAdvanceId(advanceId: Long): OffenderAdvanceWithStatus?

  @Query(
    ADVANCE_STATUS_SELECT +
      """
      WHERE od.offender_deduction_id = :deductionId
        AND $ADVANCE_BASE_CONDITIONS
    """,
    nativeQuery = true,
  )
  fun findByDeductionId(deductionId: Long): OffenderAdvanceWithStatus?

  @Query(
    ADVANCE_STATUS_SELECT +
      """
      WHERE opp.offender_id = :offenderId
        AND $ADVANCE_BASE_CONDITIONS
      ORDER BY opp.offender_payment_profile_id
    """,
    nativeQuery = true,
  )
  fun findAdvancesByOffenderId(offenderId: Long): List<OffenderAdvanceWithStatus>

  @Query(
    ADVANCE_STATUS_SELECT +
      """
            WHERE opp.offender_id = :offenderId
        AND $ACTIVE_ADVANCE_CONDITIONS
      ORDER BY opp.offender_payment_profile_id
    """,
    nativeQuery = true,
  )
  fun findActiveAdvancesByOffenderId(offenderId: Long): List<OffenderAdvanceWithStatus>

  @Query(
    """
      SELECT COUNT(opp.offender_payment_profile_id)
      FROM offender_payment_profiles opp
      JOIN offender_deductions od 
        ON opp.offender_payment_profile_id = od.offender_payment_profile_id
      WHERE
        $ACTIVE_ADVANCE_CONDITIONS
    """,
    nativeQuery = true,
  )
  fun findActiveAdvancesCount(): Long
}

interface OffenderAdvanceWithStatus {
  val id: Long
  val prisonNumber: String
  val caseloadId: String
  val advanceAmount: BigDecimal
  val advanceDate: LocalDateTime?
  val startDate: LocalDateTime
  val paymentAmount: BigDecimal
  val reference: String?
  val commentText: String?
  val createdBy: String
  val createDatetime: LocalDateTime
  val informationNumber: String
  val deductionAmount: BigDecimal?
  val adjustmentReason: AdjustmentReason?
  val adjustmentAmount: BigDecimal?
  val status: String
  val advanceCreateTransactionId: Long
}
