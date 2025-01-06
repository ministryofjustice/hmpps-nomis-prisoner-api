package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransactionDetail
import java.math.BigDecimal
import java.time.LocalDate

@DslMarker
annotation class OffenderTransactionDetailDslMarker

@NomisDataDslMarker
interface OffenderTransactionDetailDsl

@Component
class OffenderTransactionDetailBuilderFactory {
  fun builder() = OffenderTransactionDetailBuilder()
}

class OffenderTransactionDetailBuilder : OffenderTransactionDetailDsl {
  fun build(
    transactionDetailId: Long,
    offenderTransaction: OffenderTransaction,
    courseAttendance: OffenderCourseAttendance,
    calendarDate: LocalDate,
    payAmount: BigDecimal,
  ) = OffenderTransactionDetail(
    transactionDetailId = transactionDetailId,
    transaction = offenderTransaction,
    courseAttendance = courseAttendance,
    calendarDate = calendarDate,
    payAmount = payAmount,
  )
}
