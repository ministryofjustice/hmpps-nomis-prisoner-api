package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment

@Repository
interface OffenderScheduledPaymentRepository : JpaRepository<OffenderScheduledPayment, Long> {
  fun findByOffenderId(offenderId: Long): List<OffenderScheduledPayment>

  @Query(
    """
      from OffenderScheduledPayment
      where 
        offender.id = :offenderId
        and (:activeOnly = false or endDate is null or endDate > current_date)  
      order by id
    """,
  )
  fun findScheduledPayments(offenderId: Long, activeOnly: Boolean): List<OffenderScheduledPayment>

  @Query(
    """
      select count(*)
      from OffenderScheduledPayment
      where
        endDate is null or endDate > current_date
    """,
  )
  fun getActiveScheduledPaymentsCount(): Long
}
