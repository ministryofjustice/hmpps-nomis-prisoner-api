package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCategoryType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceCalculationTypeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderSentenceDslMarker

@NomisDataDslMarker
interface OffenderSentenceDsl {
  @OffenderSentenceAdjustmentDslMarker
  fun adjustment(
    adjustmentTypeCode: String = "UR",
    adjustmentDate: LocalDate = LocalDate.now(),
    createdDate: LocalDateTime = LocalDateTime.now(),
    adjustmentNumberOfDays: Long = 10,
    keyDateAdjustmentId: Long? = null,
    active: Boolean = true,
    dsl: OffenderSentenceAdjustmentDsl.() -> Unit = {},
  ): OffenderSentenceAdjustment
}

@Component
class OffenderSentenceBuilderRepository(
  val sentenceCalculationTypeRepository: SentenceCalculationTypeRepository,
  val sentenceCategoryTypeRepository: ReferenceCodeRepository<SentenceCategoryType>,
) {
  fun lookupSentenceCalculationType(calculationType: String, category: String): SentenceCalculationType =
    sentenceCalculationTypeRepository.findByIdOrNull(SentenceCalculationTypeId(calculationType, category))!!

  fun lookupSentenceCategoryType(code: String): SentenceCategoryType =
    sentenceCategoryTypeRepository.findByIdOrNull(SentenceCategoryType.pk(code))!!
}

@Component
class OffenderSentenceBuilderFactory(
  private val sentenceAdjustmentBuilderFactory: OffenderSentenceAdjustmentBuilderFactory,
  private val repository: OffenderSentenceBuilderRepository,
) {
  fun builder(): OffenderSentenceBuilder {
    return OffenderSentenceBuilder(sentenceAdjustmentBuilderFactory, repository)
  }
}

class OffenderSentenceBuilder(
  private val sentenceAdjustmentBuilderFactory: OffenderSentenceAdjustmentBuilderFactory,
  private val repository: OffenderSentenceBuilderRepository,
) : OffenderSentenceDsl {
  private lateinit var offenderSentence: OffenderSentence

  fun build(
    calculationType: String,
    category: String,
    startDate: LocalDate,
    status: String,
    sentenceLevel: String,
    offenderBooking: OffenderBooking,
    sequence: Long,
    consecSequence: Int?,
    courtOrder: Long?,
    endDate: LocalDate,
    commentText: String?,
    absenceCount: Int?,
    etdCalculatedDate: LocalDate?,
    mtdCalculatedDate: LocalDate?,
    ltdCalculatedDate: LocalDate?,
    ardCalculatedDate: LocalDate?,
    crdCalculatedDate: LocalDate?,
    pedCalculatedDate: LocalDate?,
    npdCalculatedDate: LocalDate?,
    ledCalculatedDate: LocalDate?,
    sedCalculatedDate: LocalDate?,
    prrdCalculatedDate: LocalDate?,
    tariffCalculatedDate: LocalDate?,
    dprrdCalculatedDate: LocalDate?,
    tusedCalculatedDate: LocalDate?,
    aggAdjustDays: Int?,
    aggSentenceSequence: Int?,
    extendedDays: Int?,
    counts: Int?,
    statusUpdateReason: String?,
    statusUpdateComment: String?,
    statusUpdateDate: LocalDate?,
    statusUpdateStaff: Staff?,
    fineAmount: BigDecimal?,
    dischargeDate: LocalDate?,
    nomSentDetailRef: Long?,
    nomConsToSentDetailRef: Long?,
    nomConsFromSentDetailRef: Long?,
    nomConsWithSentDetailRef: Long?,
    lineSequence: Int?,
    hdcExclusionFlag: Boolean?,
    hdcExclusionReason: String?,
    cjaAct: String?,
    sled2Calc: LocalDate?,
    startDate2Calc: LocalDate?,
  ): OffenderSentence = OffenderSentence(
    id = SentenceId(offenderBooking = offenderBooking, sequence = sequence),
    status = status,
    startDate = startDate,
    calculationType = repository.lookupSentenceCalculationType(calculationType, category),
    category = repository.lookupSentenceCategoryType(category),
    sentenceLevel = sentenceLevel,
    consecSequence = consecSequence,
    // courtOrder = courtOrder,
    endDate = endDate,
    commentText = commentText,
    absenceCount = absenceCount,
    etdCalculatedDate = etdCalculatedDate,
    mtdCalculatedDate = mtdCalculatedDate,
    ltdCalculatedDate = ltdCalculatedDate,
    ardCalculatedDate = ardCalculatedDate,
    crdCalculatedDate = crdCalculatedDate,
    pedCalculatedDate = pedCalculatedDate,
    npdCalculatedDate = npdCalculatedDate,
    ledCalculatedDate = ledCalculatedDate,
    sedCalculatedDate = sedCalculatedDate,
    prrdCalculatedDate = prrdCalculatedDate,
    tariffCalculatedDate = tariffCalculatedDate,
    dprrdCalculatedDate = dprrdCalculatedDate,
    tusedCalculatedDate = tusedCalculatedDate,
    aggAdjustDays = aggAdjustDays,
    aggSentenceSequence = aggSentenceSequence,
    extendedDays = extendedDays,
    counts = counts,
    statusUpdateReason = statusUpdateReason,
    statusUpdateComment = statusUpdateComment,
    statusUpdateDate = statusUpdateDate,
    statusUpdateStaff = statusUpdateStaff,
    fineAmount = fineAmount,
    dischargeDate = dischargeDate,
    nomSentDetailRef = nomSentDetailRef,
    nomConsToSentDetailRef = nomConsToSentDetailRef,
    nomConsFromSentDetailRef = nomConsFromSentDetailRef,
    nomConsWithSentDetailRef = nomConsWithSentDetailRef,
    lineSequence = lineSequence,
    hdcExclusionFlag = hdcExclusionFlag,
    hdcExclusionReason = hdcExclusionReason,
    cjaAct = cjaAct,
    sled2Calc = sled2Calc,
    startDate2Calc = startDate2Calc,
  )
    .also { offenderSentence = it }

  override fun adjustment(
    adjustmentTypeCode: String,
    adjustmentDate: LocalDate,
    createdDate: LocalDateTime,
    adjustmentNumberOfDays: Long,
    keyDateAdjustmentId: Long?,
    active: Boolean,
    dsl: OffenderSentenceAdjustmentDsl.() -> Unit,
  ): OffenderSentenceAdjustment = sentenceAdjustmentBuilderFactory.builder().let { builder ->
    builder.build(
      adjustmentTypeCode = adjustmentTypeCode,
      adjustmentDate = adjustmentDate,
      createdDate = createdDate,
      adjustmentNumberOfDays = adjustmentNumberOfDays,
      keyDateAdjustmentId = keyDateAdjustmentId,
      active = active,
      sentence = offenderSentence,
    )
      .also { offenderSentence.adjustments += it }
      .also { builder.apply(dsl) }
  }
}
