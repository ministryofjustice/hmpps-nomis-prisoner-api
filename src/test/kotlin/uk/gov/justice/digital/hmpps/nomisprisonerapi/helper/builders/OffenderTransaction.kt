package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransactionDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransactionId
import java.math.BigDecimal

@DslMarker
annotation class OffenderTransactionDslMarker

@NomisDataDslMarker
interface OffenderTransactionDsl {
  @OffenderTransactionDetailDslMarker
  fun detail(
    transactionDetailId: Long = 0,
    payAmount: Int,
  ): OffenderTransactionDetail
}

@Component
class OffenderTransactionBuilderFactory(
  private val detailBuilderFactory: OffenderTransactionDetailBuilderFactory,
) {
  fun builder() = OffenderTransactionBuilder(detailBuilderFactory)
}

class OffenderTransactionBuilder(
  private val detailBuilderFactory: OffenderTransactionDetailBuilderFactory,
) : OffenderTransactionDsl {

  private lateinit var courseAttendance: OffenderCourseAttendance
  private lateinit var offenderTransaction: OffenderTransaction

  fun build(
    offenderCourseAttendance: OffenderCourseAttendance,
    transactionId: Long,
    entrySeq: Long,
    caseloadId: String,
    offenderId: Long,
  ): OffenderTransaction = OffenderTransaction(
    id = OffenderTransactionId(transactionId, entrySeq),
    caseloadId = caseloadId,
    offenderId = offenderId,
  ).also {
    courseAttendance = offenderCourseAttendance
    offenderTransaction = it
  }

  override fun detail(
    transactionDetailId: Long,
    payAmount: Int,
  ) =
    detailBuilderFactory.builder().build(
      transactionDetailId = transactionDetailId,
      offenderTransaction = offenderTransaction,
      courseAttendance = courseAttendance,
      calendarDate = courseAttendance.eventDate,
      payAmount = BigDecimal.valueOf(payAmount.toLong(), 2),
    ).also {
      offenderTransaction.transactionDetails += it
    }
}
