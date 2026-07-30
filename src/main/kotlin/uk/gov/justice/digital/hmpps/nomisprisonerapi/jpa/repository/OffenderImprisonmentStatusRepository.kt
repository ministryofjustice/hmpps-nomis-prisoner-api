package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImprisonmentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImprisonmentStatusId

@Repository
interface OffenderImprisonmentStatusRepository : JpaRepository<OffenderImprisonmentStatus, OffenderImprisonmentStatusId> {

  @Query(
    """
SELECT 
        imprisonment_status, 
        offender_charge_id
           FROM (SELECT os.sentence_seq,
                        os.end_date - os.start_date sent_length,
                        ips.imprisonment_status,ips.rank_value,
                        oc.offender_charge_id,
                        o.severity_ranking,
                        MIN(ips.rank_value) OVER(PARTITION BY os.offender_book_id) min_rank,
                        MAX(os.end_date - os.start_date) 
                            OVER(PARTITION BY os.offender_book_id,ips.rank_value) max_len,
                        MIN(TO_NUMBER(o.severity_ranking)) 
                            OVER(PARTITION BY os.offender_book_id,ips.rank_value,
                                              os.end_date - os.start_date) min_off_rank,
                        MIN(os.sentence_seq)
                            OVER(PARTITION BY os.offender_book_id,ips.rank_value,
                                              os.end_date - os.start_date,o.severity_ranking) min_sent_seq,
                        MIN(oc.offender_charge_id) 
                            OVER(PARTITION BY os.offender_book_id,ips.rank_value,
                                              os.end_date - os.start_date,o.severity_ranking,
                                              os.sentence_seq) min_charge_id
                   FROM offender_bookings ob
                   JOIN offender_cases oca
                     ON oca.offender_book_id = ob.offender_book_id
                        AND oca.case_status = 'A'
                   JOIN offender_sentences os
                     ON os.case_id = oca.case_id
                        AND os.sentence_status = 'A'
                   JOIN imprison_status_mappings ipsm
                     ON ipsm.sentence_category = os.sentence_category
                        AND ipsm.sentence_calc_type = os.sentence_calc_type
                        AND ipsm.active_flag = 'Y'
                   JOIN imprisonment_statuses ips
                     ON ips.imprisonment_status_id = ipsm.imprisonment_status_id
                   JOIN offender_sentence_charges osc
                     ON osc.offender_book_id = os.offender_book_id
                        AND osc.sentence_seq = os.sentence_seq
                   JOIN offender_charges oc
                     ON oc.offender_charge_id = osc.offender_charge_id
                   JOIN offences o
                     ON o.statute_code = oc.statute_code
                        AND o.offence_code = oc.offence_code
                  WHERE ob.offender_book_id = :bookingId)
          WHERE rank_value = min_rank
            AND ((sent_length IS NULL AND max_len IS NULL)
                 OR (sent_length = max_len))
            AND severity_ranking = min_off_rank      
            AND sentence_seq = min_sent_seq
            AND offender_charge_id = min_charge_id
                """,
    nativeQuery = true,
  )
  fun getStatusAndMainOffenceViaSentenceByBookingId(bookingId: Long): StatusAndMainOffence?

  @Query(
    """
SELECT NVL(imprisonment_status,'UNKNOWN'),
                   offender_charge_id
              FROM (SELECT oc.offender_charge_id,
                           ips.imprisonment_status,
                           ips.rank_value,
                           o.severity_ranking,
                           MIN(ips.rank_value) OVER(PARTITION BY oc.offender_book_id) min_rank,
                           MIN(TO_NUMBER(o.severity_ranking)) 
                               OVER(PARTITION BY oc.offender_book_id,ips.rank_value) min_off_rank,
                           MIN(oc.offender_charge_id)
                               OVER(PARTITION BY oc.offender_book_id,ips.rank_value,o.severity_ranking) min_charge_id
                      FROM offender_bookings ob
                      LEFT OUTER JOIN (offender_cases oca
                            JOIN (offender_charges oc
                                  LEFT OUTER JOIN imprison_status_mappings ipsm
                                    ON ipsm.offence_result_code = oc.result_code_1
                                       AND ipsm.active_flag = 'Y'
                                  LEFT OUTER JOIN imprisonment_statuses ips
                                    ON ips.imprisonment_status_id = ipsm.imprisonment_status_id
                                  JOIN offences o
                                    ON o.statute_code = oc.statute_code
                                       AND o.offence_code = oc.offence_code)
                              ON oc.case_id = oca.case_id)
                        ON oca.offender_book_id = ob.offender_book_id
                           AND oca.case_status = 'A'
                    WHERE ob.offender_book_id = :bookingId)
             WHERE NVL(rank_value,-1) = NVL(min_rank,-1)
               AND (severity_ranking IS NULL OR severity_ranking = min_off_rank)
               AND (offender_charge_id IS NULL OR offender_charge_id = min_charge_id)                
               """,
    nativeQuery = true,
  )
  fun getStatusAndMainOffenceViaChargeOutcomeByBookingId(bookingId: Long): StatusAndMainOffence

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000")])
  @Query("from OffenderImprisonmentStatus ois where ois.id = :status")
  fun findByIdWaitForLock(status: OffenderImprisonmentStatusId): OffenderImprisonmentStatus
}

data class StatusAndMainOffence(val imprisonmentStatus: String, val offenderChargeId: Number?)
