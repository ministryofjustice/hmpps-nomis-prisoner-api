package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@DiscriminatorValue(OffenderAdvance.PAYMENT_MODE)
class OffenderAdvance(
  offender: Offender,
  caseloadId: String,
  transactionType: String,
  startDate: LocalDate = LocalDate.now(),
  endDate: LocalDate? = null,
  paymentAmount: BigDecimal,
  referenceText: String? = null,
  commentText: String? = null,
  recordUserId: String? = null,

  @Column
  var advanceAmount: BigDecimal,
  @Column
  var advanceDate: LocalDate = LocalDate.now(),

  @OneToMany(mappedBy = "offenderAdvance")
  val deductions: MutableList<OffenderDeduction> = mutableListOf(),
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
    const val PAYMENT_MODE = "ADVANCE"
  }
}
