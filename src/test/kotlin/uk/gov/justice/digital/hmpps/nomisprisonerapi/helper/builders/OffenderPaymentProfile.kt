package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadDeductionProfileId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderDeduction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadDeductionProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAdvanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledPaymentRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderPaymentProfileDslMarker

@NomisDataDslMarker
interface OffenderPaymentProfileDsl

@Component
class OffenderPaymentProfileBuilderFactory(
  private val repository: OffenderDeductionBuilderRepository,
  private val offenderTrustAccountRepository: OffenderTrustAccountBuilderRepository,
  private val offenderScheduledPaymentRepository: OffenderScheduledPaymentRepository,
  private val entityManager: EntityManager,

) {
  fun builder() = OffenderPaymentProfileBuilder(repository, offenderTrustAccountRepository, offenderScheduledPaymentRepository, entityManager)
}

@Component
class OffenderDeductionBuilderRepository(
  val offenderAdvanceRepository: OffenderAdvanceRepository,
  val caseloadDeductionProfileRepository: CaseloadDeductionProfileRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun lookupCaseloadDeductionProfile(caseloadId: String, deductionType: String) = caseloadDeductionProfileRepository.findByIdOrNull(
    CaseloadDeductionProfileId(caseloadId, deductionType),
  )
    ?: throw IllegalArgumentException("No CaseloadDeductionProfile found for caseloadId=$caseloadId and deductionType=$deductionType")

  fun save(advance: OffenderAdvance): OffenderAdvance = offenderAdvanceRepository.saveAndFlush(advance)
  fun updateModifiedDatetime(payementProfileId: Long, whenUpdated: LocalDateTime) {
    jdbcTemplate.update("update OFFENDER_PAYMENT_PROFILES set MODIFY_DATETIME = ? where OFFENDER_PAYMENT_PROFILE_ID = ?", whenUpdated, payementProfileId)
  }
  fun updateModifiedUsername(payementProfileId: Long, whoUpdated: String) {
    jdbcTemplate.update("update OFFENDER_PAYMENT_PROFILES set MODIFY_USER_ID = ? where OFFENDER_PAYMENT_PROFILE_ID = ?", whoUpdated, payementProfileId)
  }
}

class OffenderPaymentProfileBuilder(
  private val offenderDeductionBuilderRepository: OffenderDeductionBuilderRepository,
  private val offenderTrustAccountRepository: OffenderTrustAccountBuilderRepository,
  private val offenderScheduledPaymentRepository: OffenderScheduledPaymentRepository,
  private val entityManager: EntityManager,
) : OffenderPaymentProfileDsl {

  fun buildAdvance(
    offender: Offender,
    caseloadId: String,
    transactionType: String,
    advanceAmount: BigDecimal,
    paymentAmount: BigDecimal,
    advanceDate: LocalDate,
    deductionAmount: BigDecimal,
    deductionPriority: Int,
    informationNumber: String,
    writeOffAdjustmentAmount: BigDecimal?,
    whenModified: LocalDateTime?,
    whoModified: String?,
  ): OffenderAdvance {
    val caseloadDeductionProfile = offenderDeductionBuilderRepository.lookupCaseloadDeductionProfile(caseloadId, "ADV")
    val offenderTrustAccount = offenderTrustAccountRepository.lookup(offender, caseloadId)

    val od = OffenderDeduction(
      caseloadDeductionProfile = caseloadDeductionProfile,
      offenderTrustAccount = offenderTrustAccount,
      deductionPriority = deductionPriority,
      effectiveDate = LocalDate.now(),
      deductionAmount = deductionAmount,
      deductionStatus = "A",
      deductionType = "ADV",
      deductionPercentage = 20,
      informationNumber = informationNumber,
      maxTotalAmount = advanceAmount,
      adjustmentAmount = writeOffAdjustmentAmount,
      adjustmentReason = writeOffAdjustmentAmount?.let { AdjustmentReason.WRT },
    )
    val advance = OffenderAdvance(
      offender = offender,
      caseloadId = caseloadId,
      transactionType = transactionType,
      advanceAmount = advanceAmount,
      paymentAmount = paymentAmount,
      advanceDate = advanceDate,
      deduction = od,
    )
    od.offenderAdvance = advance
    return offenderDeductionBuilderRepository.save(advance).also { advance ->
      if (whenModified != null) {
        offenderDeductionBuilderRepository.updateModifiedDatetime(advance.id, whenModified)
      }
      if (whoModified != null) {
        offenderDeductionBuilderRepository.updateModifiedUsername(advance.id, whoModified)
      }
      if (whenModified != null || whoModified != null) {
        entityManager.refresh(advance)
      }
    }
  }

  fun buildScheduledPayment(
    offender: Offender,
    caseloadId: String,
    transactionType: String,
    startDate: LocalDate,
    endDate: LocalDate?,
    reference: String?,
    weeklyPaymentAmount: BigDecimal,
    comment: String?,
    whenModified: LocalDateTime? = null,
    whoModified: String? = null,
  ): OffenderScheduledPayment = OffenderScheduledPayment(
    offender = offender,
    caseloadId = caseloadId,
    transactionType = transactionType,
    paymentAmount = weeklyPaymentAmount,
    startDate = startDate,
    endDate = endDate,
    referenceText = reference,
    commentText = comment,
  ).let { offenderScheduledPaymentRepository.saveAndFlush(it) }
    .also { payment ->
      if (whenModified != null) {
        offenderDeductionBuilderRepository.updateModifiedDatetime(payment.id, whenModified)
      }
      if (whoModified != null) {
        offenderDeductionBuilderRepository.updateModifiedUsername(payment.id, whoModified)
      }
      if (whenModified != null || whoModified != null) {
        entityManager.refresh(payment)
      }
    }
}
