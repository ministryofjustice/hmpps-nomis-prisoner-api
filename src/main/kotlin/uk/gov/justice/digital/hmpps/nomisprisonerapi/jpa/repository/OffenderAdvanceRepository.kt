package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance

@Repository
interface OffenderAdvanceRepository : JpaRepository<OffenderAdvance, Long> {
  fun findByOffenderId(offenderId: Long): List<OffenderAdvance>

  @Query(
    """
      SELECT
        opp.*
      FROM 
        offender_payment_profiles opp
        inner join  offender_deductions od on opp.offender_payment_profile_id = od.offender_payment_profile_id
      WHERE 
        opp.offender_id = :offenderId
        AND opp.start_date <= TRUNC (SYSDATE)
        AND opp.payment_amount > 0
        AND od.effective_date <= TRUNC (SYSDATE)
        AND od.deduction_status = 'A'
        AND od.max_total_amount > (NVL (od.deduction_amount, 0) + NVL(od.adjustment_amount,0))
        AND GREATEST ( NVL (od.max_total_amount, 0) - (NVL (od.deduction_amount, 0) + NVL(od.adjustment_amount,0)), 0) > 0
      ORDER BY opp.offender_payment_profile_id
    """,
    nativeQuery = true,
  )
  fun findActiveAdvancesByOffenderId(offenderId: Long): List<OffenderAdvance>

  @Query(
    """
      SELECT
          count(*)
        FROM 
          offender_payment_profiles opp
          inner join  offender_deductions od on opp.offender_payment_profile_id = od.offender_payment_profile_id
      WHERE 
          opp.start_date <= TRUNC (SYSDATE)
          AND opp.payment_amount > 0
          AND od.effective_date <= TRUNC (SYSDATE)
          AND od.deduction_status = 'A'
          AND od.max_total_amount > (NVL (od.deduction_amount, 0) + NVL(od.adjustment_amount,0))
          AND GREATEST ( NVL (od.max_total_amount, 0) - (NVL (od.deduction_amount, 0) + NVL(od.adjustment_amount,0)), 0) > 0
    """,
    nativeQuery = true,
  )
  fun findActiveAdvancesCount(): Long
}
