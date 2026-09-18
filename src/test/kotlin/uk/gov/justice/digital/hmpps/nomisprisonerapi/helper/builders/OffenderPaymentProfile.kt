package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import java.math.BigDecimal
import java.time.LocalDate

@DslMarker
annotation class OffenderPaymentProfileDslMarker

@NomisDataDslMarker
interface OffenderPaymentProfileDsl

@Component
class OffenderPaymentProfileBuilderFactory {
  fun builder() = OffenderPaymentProfileBuilder()
}

class OffenderPaymentProfileBuilder : OffenderPaymentProfileDsl {

  fun buildAdvance(
    offender: Offender,
    caseloadId: String,
    transactionType: String,
  ): OffenderAdvance = OffenderAdvance(
    offender = offender,
    caseloadId = caseloadId,
    transactionType = transactionType,
    advanceAmount = BigDecimal.valueOf(12.45),
    paymentAmount = BigDecimal.valueOf(2.45),
  )

  fun buildScheduledPayment(
    offender: Offender,
    caseloadId: String,
    transactionType: String,
    endDate: LocalDate?,
    reference: String?,
    comment: String?,
  ): OffenderScheduledPayment = OffenderScheduledPayment(
    offender = offender,
    caseloadId = caseloadId,
    transactionType = transactionType,
    paymentAmount = BigDecimal.valueOf(2.45),
    endDate = endDate,
    referenceText = reference,
    commentText = comment,
  )
}
