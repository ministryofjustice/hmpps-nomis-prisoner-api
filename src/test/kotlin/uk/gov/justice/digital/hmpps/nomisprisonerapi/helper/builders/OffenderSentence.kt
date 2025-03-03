package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTerm
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCategoryType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
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

  @OffenderSentenceTermDslMarker
  fun term(
    startDate: LocalDate = LocalDate.of(2023, 1, 1),
    endDate: LocalDate = LocalDate.of(2023, 1, 5),
    years: Int? = 2,
    months: Int? = 3,
    weeks: Int? = 4,
    days: Int? = 5,
    hours: Int? = 6,
    sentenceTermType: String = "SEC86",
    lifeSentenceFlag: Boolean = true,
    dsl: OffenderSentenceTermDsl.() -> Unit = {},
  ): OffenderSentenceTerm

  @OffenderSentenceChargeDslMarker
  fun offenderSentenceCharge(
    offenderCharge: OffenderCharge,
    dsl: OffenderSentenceChargeDsl.() -> Unit = {},
  ): OffenderSentenceCharge
}

@Component
class OffenderSentenceBuilderRepository(
  val sentenceCalculationTypeRepository: SentenceCalculationTypeRepository,
  val sentenceCategoryTypeRepository: ReferenceCodeRepository<SentenceCategoryType>,
  val offenderSentenceRepository: OffenderSentenceRepository,
) {
  fun lookupSentenceCalculationType(calculationType: String, category: String): SentenceCalculationType = sentenceCalculationTypeRepository.findByIdOrNull(SentenceCalculationTypeId(calculationType, category))!!

  fun lookupSentenceCategoryType(code: String): SentenceCategoryType = sentenceCategoryTypeRepository.findByIdOrNull(SentenceCategoryType.pk(code))!!

  fun save(sentence: OffenderSentence): OffenderSentence = offenderSentenceRepository.save(sentence)
}

@Component
class OffenderSentenceBuilderFactory(
  private val sentenceTermBuilderFactory: OffenderSentenceTermBuilderFactory,
  private val sentenceChargeBuilderFactory: OffenderSentenceChargeBuilderFactory,
  private val sentenceAdjustmentBuilderFactory: OffenderSentenceAdjustmentBuilderFactory,
  private val repository: OffenderSentenceBuilderRepository,
) {
  fun builder(): OffenderSentenceBuilder = OffenderSentenceBuilder(
    sentenceAdjustmentBuilderFactory,
    sentenceTermBuilderFactory,
    sentenceChargeBuilderFactory,
    repository,
  )
}

class OffenderSentenceBuilder(
  private val sentenceAdjustmentBuilderFactory: OffenderSentenceAdjustmentBuilderFactory,
  private val sentenceTermBuilderFactory: OffenderSentenceTermBuilderFactory,
  private val sentenceChargeBuilderFactory: OffenderSentenceChargeBuilderFactory,
  private val repository: OffenderSentenceBuilderRepository,
) : OffenderSentenceDsl {
  private lateinit var offenderSentence: OffenderSentence

  fun build(
    courtCase: CourtCase?,
    calculationType: String,
    category: String,
    startDate: LocalDate,
    status: String,
    sentenceLevel: String,
    offenderBooking: OffenderBooking,
    sequence: Long,
    consecLineSequence: Int?,
    courtOrder: CourtOrder?,
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
    courtCase = courtCase,
    status = status,
    startDate = startDate,
    calculationType = repository.lookupSentenceCalculationType(calculationType, category),
    category = repository.lookupSentenceCategoryType(category),
    sentenceLevel = sentenceLevel,
    consecSequence = consecLineSequence,
    courtOrder = courtOrder,
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
    .let { repository.save(it) }
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

  override fun term(
    startDate: LocalDate,
    endDate: LocalDate,
    years: Int?,
    months: Int?,
    weeks: Int?,
    days: Int?,
    hours: Int?,
    sentenceTermType: String,
    lifeSentenceFlag: Boolean,
    dsl: OffenderSentenceTermDsl.() -> Unit,
  ): OffenderSentenceTerm = sentenceTermBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = offenderSentence.id.offenderBooking,
      termSequence = (offenderSentence.offenderSentenceTerms.size + 1).toLong(),
      startDate = startDate,
      endDate = endDate,
      years = years,
      months = months,
      weeks = weeks,
      days = days,
      hours = hours,
      sentenceTermType = sentenceTermType,
      lifeSentenceFlag = lifeSentenceFlag,
      sentence = offenderSentence,
    )
      .also { offenderSentence.offenderSentenceTerms += it }
      .also { builder.apply(dsl) }
  }

  override fun offenderSentenceCharge(
    offenderCharge: OffenderCharge,
    dsl: OffenderSentenceChargeDsl.() -> Unit,
  ): OffenderSentenceCharge = sentenceChargeBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = offenderSentence.id.offenderBooking,
      offenderCharge = offenderCharge,
      sentence = offenderSentence,
    )
      .also { offenderSentence.offenderSentenceCharges += it }
      .also { builder.apply(dsl) }
  }
}
