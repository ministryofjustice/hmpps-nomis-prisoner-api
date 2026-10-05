package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadDeductionProfileId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderDeduction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadDeductionProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAdvanceRepository
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
  val offenderAdvanceRepository: OffenderAdvanceRepository,
  val caseloadDeductionProfileRepository: CaseloadDeductionProfileRepository,
) {
  fun lookupCaseloadDeductionProfile(caseloadId: String, deductionType: String) = caseloadDeductionProfileRepository.findByIdOrNull(
    CaseloadDeductionProfileId(caseloadId, deductionType),
  )
    ?: throw IllegalArgumentException("No CaseloadDeductionProfile found for caseloadId=$caseloadId and deductionType=$deductionType")

  fun save(advance: OffenderAdvance): OffenderAdvance = offenderAdvanceRepository.saveAndFlush(advance)
}

class OffenderPaymentProfileBuilder(
  private val offenderDeductionBuilderRepository: OffenderDeductionBuilderRepository,
) : OffenderPaymentProfileDsl {

  fun buildAdvance(
    offender: Offender,
    caseloadId: String,
    transactionType: String,
    deductionPriority: Int,
    informationNumber: String?,
  ): OffenderAdvance {
    val caseloadDeductionProfile = offenderDeductionBuilderRepository.lookupCaseloadDeductionProfile(caseloadId, "ADV")
    val od = OffenderDeduction(
      caseloadDeductionProfile = caseloadDeductionProfile,
      offender = offender,
      deductionPriority = deductionPriority,
      effectiveDate = LocalDate.now(),
      deductionPercentage = 20,
      informationNumber = informationNumber,
    )
    return offenderDeductionBuilderRepository.save(
      OffenderAdvance(
        offender = offender,
        caseloadId = caseloadId,
        transactionType = transactionType,
        advanceAmount = BigDecimal.valueOf(12.45),
        paymentAmount = BigDecimal.valueOf(2.45),
        deductions = mutableListOf(od),
      ),
    ).also {
      it.deductions.first().offenderAdvance = it
    }
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
