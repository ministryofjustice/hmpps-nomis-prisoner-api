package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import java.math.BigDecimal
import java.time.LocalDate

private const val SCHEDULED_PAYMENT_SELECT = """
      select
        osc.id as id,
        osc.offender.nomsId as prisonNumber,
        osc.caseloadId as caseloadId,
        osc.transactionType as transactionType,
        osc.startDate as startDate,
        osc.endDate as endDate,
        osc.paymentAmount as paymentAmount,
        osc.referenceText as referenceText,
        osc.commentText as commentText,
        osc.createUsername as createUsername,
        cpp.weekDay as weekDay
      from OffenderScheduledPayment osc
      join CaseloadPaymentProfile cpp
        on osc.caseloadId = cpp.caseloadId
        and cpp.paymentMode = 'SCHEDPAY'
        and osc.transactionType = cpp.transactionType
        and cpp.active = true
"""

@Repository
interface OffenderScheduledPaymentRepository : JpaRepository<OffenderScheduledPayment, Long> {
  @Query(
    SCHEDULED_PAYMENT_SELECT + """
      where osc.id = :scheduledPaymentId
    """,
  )
  fun findScheduledPaymentById(scheduledPaymentId: Long): ScheduledPaymentProjection?

  @Query(
    SCHEDULED_PAYMENT_SELECT + """
      where
        osc.offender.id = :offenderId
        and (:activeOnly = false or osc.endDate is null or osc.endDate > current_date)  
      order by osc.id
    """,
  )
  fun findScheduledPayments(offenderId: Long, activeOnly: Boolean): List<ScheduledPaymentProjection>

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

interface ScheduledPaymentProjection {
  val id: Long
  val prisonNumber: String
  val caseloadId: String
  val transactionType: String
  val startDate: LocalDate
  val endDate: LocalDate?
  val paymentAmount: BigDecimal
  val referenceText: String?
  val commentText: String?
  val createUsername: String
  val weekDay: String
}
