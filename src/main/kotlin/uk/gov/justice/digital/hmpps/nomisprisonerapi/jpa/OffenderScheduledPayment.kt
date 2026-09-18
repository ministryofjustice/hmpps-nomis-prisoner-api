package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@DiscriminatorValue(OffenderScheduledPayment.PAYMENT_MODE)
class OffenderScheduledPayment(
  offender: Offender,
  caseloadId: String,
  transactionType: String,
  startDate: LocalDate = LocalDate.now(),
  endDate: LocalDate? = null,
  paymentAmount: BigDecimal,
  referenceText: String? = null,
  commentText: String? = null,
  recordUserId: String? = null,

) : OffenderPaymentProfile(
  offender = offender,
  caseloadId = caseloadId,
  transactionType = transactionType,
  startDate = startDate,
  endDate = endDate,
  paymentAmount = paymentAmount,
  referenceText = referenceText,
  commentText = commentText,
  recordUserId = recordUserId,
) {
  companion object {
    const val PAYMENT_MODE = "SCHEDPAY"
  }
}
