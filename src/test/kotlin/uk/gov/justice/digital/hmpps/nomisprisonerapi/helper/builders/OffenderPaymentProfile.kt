package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderDeduction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderDeductionRepository
import java.math.BigDecimal
import java.time.LocalDate

@DslMarker
annotation class OffenderPaymentProfileDslMarker

@NomisDataDslMarker
interface OffenderPaymentProfileDsl

@Component
class OffenderPaymentProfileBuilderFactory(
  private val repository: OffenderDeductionBuilderRepository,
) {
  fun builder() = OffenderPaymentProfileBuilder(repository)
}

@Component
class OffenderDeductionBuilderRepository(
  val offenderDeductionRepository: OffenderDeductionRepository,
) {
  fun save(deduction: OffenderDeduction): OffenderDeduction = offenderDeductionRepository.save(deduction)
}

class OffenderPaymentProfileBuilder(
  private val offenderDeductionBuilderRepository: OffenderDeductionBuilderRepository,
) : OffenderPaymentProfileDsl {

  fun buildAdvance(
    offender: Offender,
    caseloadId: String,
    transactionType: String,
    informationNumber: String?,
  ): OffenderAdvance {
    val od = offenderDeductionBuilderRepository.save(
      OffenderDeduction(
        caseloadId = caseloadId,
        offender = offender,
        deductionPriority = 1,
        effectiveDate = LocalDate.now(),
        deductionPercentage = 20,
        informationNumber = informationNumber,
      ),
    )
    return OffenderAdvance(
      offender = offender,
      caseloadId = caseloadId,
      transactionType = transactionType,
      advanceAmount = BigDecimal.valueOf(12.45),
      paymentAmount = BigDecimal.valueOf(2.45),
      deductions = mutableListOf(od),
    )
  }

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
