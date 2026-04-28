package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceStatus
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

@DslMarker
annotation class OffenderSentenceLicenceDslMarker

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
    // will use the court order date and null end date for dps created sentences
    startDate: LocalDate = LocalDate.of(2023, 1, 1),
    endDate: LocalDate? = null,
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

  @OffenderSentenceStatusDslMarker
  fun offenderSentenceStatus(
    statusUpdateStaff: Staff,
    dsl: OffenderSentenceStatusDsl.() -> Unit = {},
  ): OffenderSentenceStatus

  @OffenderSentenceStatusDslMarker
  fun audit(
    createDatetime: LocalDateTime = LocalDateTime.now(),
    createUserId: String = "ABC12A",
    modifyUserId: String? = null,
    modifyDatetime: LocalDateTime? = null,
    auditModule: String = "OCDCCASE",
  )

  @OffenderSentenceLicenceDslMarker
  fun licence(): OffenderSentence
}

@Component
class OffenderSentenceBuilderRepository(
  val sentenceCalculationTypeRepository: SentenceCalculationTypeRepository,
  val sentenceCategoryTypeRepository: ReferenceCodeRepository<SentenceCategoryType>,
  val offenderSentenceRepository: OffenderSentenceRepository,
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun lookupSentenceCalculationType(calculationType: String, category: String): SentenceCalculationType = sentenceCalculationTypeRepository.findByIdOrNull(SentenceCalculationTypeId(calculationType, category))!!

  fun lookupSentenceCategoryType(code: String): SentenceCategoryType = sentenceCategoryTypeRepository.findByIdOrNull(SentenceCategoryType.pk(code))!!

  fun save(sentence: OffenderSentence): OffenderSentence = offenderSentenceRepository.saveAndFlush(sentence)

  fun link(sentence: OffenderSentence, licence: OffenderSentence) {
    jdbcTemplate.update(
      """
      INSERT INTO OFFENDER_LICENCE_SENTENCES
      (OFFENDER_BOOK_ID, SENTENCE_SEQ, LICENCE_SENTENCE_SEQ, CASE_ID) VALUES
      (:offenderBookId, :sentenceSequence, :licenceSentenceSequence, :caseId)
      """,
      mapOf(
        "offenderBookId" to sentence.id.offenderBooking.bookingId,
        "sentenceSequence" to sentence.id.sequence,
        "licenceSentenceSequence" to licence.id.sequence,
        "caseId" to sentence.courtCase!!.id,
      ),
    )
  }

  fun updateAudit(
    id: SentenceId,
    createDatetime: LocalDateTime,
    createUserId: String,
    modifyUserId: String?,
    modifyDatetime: LocalDateTime?,
    auditModule: String,
  ) {
    jdbcTemplate.update(
      """
      UPDATE OFFENDER_SENTENCES 
      SET 
        CREATE_DATETIME = :createDatetime,
        CREATE_USER_ID = :createUserId,
        MODIFY_USER_ID = :modifyUserId,
        MODIFY_DATETIME = :modifyDatetime,
        AUDIT_MODULE_NAME = :auditModule
      WHERE OFFENDER_BOOK_ID = :offenderBookId AND SENTENCE_SEQ = :sentenceSequence 
      """,
      mapOf(
        "createDatetime" to createDatetime,
        "createUserId" to createUserId,
        "modifyUserId" to modifyUserId,
        "modifyDatetime" to modifyDatetime,
        "auditModule" to auditModule,
        "offenderBookId" to id.offenderBooking.bookingId,
        "sentenceSequence" to id.sequence,
      ),
    )
  }
}

@Component
class OffenderSentenceBuilderFactory(
  private val sentenceTermBuilderFactory: OffenderSentenceTermBuilderFactory,
  private val sentenceChargeBuilderFactory: OffenderSentenceChargeBuilderFactory,
  private val sentenceAdjustmentBuilderFactory: OffenderSentenceAdjustmentBuilderFactory,
  private val offenderSentenceStatusBuilderFactory: OffenderSentenceStatusBuilderFactory,
  private val repository: OffenderSentenceBuilderRepository,
) {
  fun builder(): OffenderSentenceBuilder = OffenderSentenceBuilder(
    sentenceAdjustmentBuilderFactory,
    sentenceTermBuilderFactory,
    sentenceChargeBuilderFactory,
    offenderSentenceStatusBuilderFactory,
    this,
    repository,
  )
}

class OffenderSentenceBuilder(
  private val sentenceAdjustmentBuilderFactory: OffenderSentenceAdjustmentBuilderFactory,
  private val sentenceTermBuilderFactory: OffenderSentenceTermBuilderFactory,
  private val sentenceChargeBuilderFactory: OffenderSentenceChargeBuilderFactory,
  private val offenderSentenceStatusBuilderFactory: OffenderSentenceStatusBuilderFactory,
  private val offenderSentenceBuilderFactory: OffenderSentenceBuilderFactory,
  private val repository: OffenderSentenceBuilderRepository,
) : OffenderSentenceDsl {
  private lateinit var offenderSentence: OffenderSentence

  fun build(
    courtCase: CourtCase? = null,
    calculationType: String,
    category: String,
    startDate: LocalDate,
    status: String,
    sentenceLevel: String,
    offenderBooking: OffenderBooking,
    sequence: Long,
    consecLineSequence: Int? = null,
    courtOrder: CourtOrder? = null,
    endDate: LocalDate? = null,
    commentText: String? = null,
    absenceCount: Int? = null,
    etdCalculatedDate: LocalDate? = null,
    mtdCalculatedDate: LocalDate? = null,
    ltdCalculatedDate: LocalDate? = null,
    ardCalculatedDate: LocalDate? = null,
    crdCalculatedDate: LocalDate? = null,
    pedCalculatedDate: LocalDate? = null,
    npdCalculatedDate: LocalDate? = null,
    ledCalculatedDate: LocalDate? = null,
    sedCalculatedDate: LocalDate? = null,
    prrdCalculatedDate: LocalDate? = null,
    tariffCalculatedDate: LocalDate? = null,
    dprrdCalculatedDate: LocalDate? = null,
    tusedCalculatedDate: LocalDate? = null,
    aggAdjustDays: Int? = null,
    aggSentenceSequence: Int? = null,
    extendedDays: Int? = null,
    counts: Int? = null,
    statusUpdateReason: String? = null,
    statusUpdateComment: String? = null,
    statusUpdateDate: LocalDate? = null,
    statusUpdateStaff: Staff? = null,
    fineAmount: BigDecimal? = null,
    dischargeDate: LocalDate? = null,
    nomSentDetailRef: Long? = null,
    nomConsToSentDetailRef: Long? = null,
    nomConsFromSentDetailRef: Long? = null,
    nomConsWithSentDetailRef: Long? = null,
    lineSequence: Int? = null,
    hdcExclusionFlag: Boolean? = null,
    hdcExclusionReason: String? = null,
    cjaAct: String? = null,
    sled2Calc: LocalDate? = null,
    startDate2Calc: LocalDate? = null,
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

  fun link(sentence: OffenderSentence, licence: OffenderSentence) {
    repository.link(sentence, licence)
  }

  override fun term(
    startDate: LocalDate,
    endDate: LocalDate?,
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
      // replicate use of courtOrder date if available (should always be available)
      startDate = offenderSentence.courtOrder?.courtDate ?: startDate,
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

  override fun offenderSentenceStatus(
    statusUpdateStaff: Staff,
    dsl: OffenderSentenceStatusDsl.() -> Unit,
  ) = offenderSentenceStatusBuilderFactory.builder().let { builder ->
    builder.build(
      sentence = offenderSentence,
      statusUpdateStaff = statusUpdateStaff,
    )
      .also { builder.apply(dsl) }
  }

  override fun licence() = offenderSentenceBuilderFactory.builder().let { builder ->
    builder.build(
      category = "LICENCE",
      startDate = LocalDate.now(),
      status = "A",
      sentenceLevel = "IND",
      offenderBooking = offenderSentence.id.offenderBooking,
      calculationType = "AP",
      sequence = offenderSentence.id.sequence + 1000,
      endDate = LocalDate.now().plusDays(1),
    )
      .also {
        builder.link(offenderSentence, it)
      }
  }

  override fun audit(
    createDatetime: LocalDateTime,
    createUserId: String,
    modifyUserId: String?,
    modifyDatetime: LocalDateTime?,
    auditModule: String,
  ) = repository.updateAudit(
    id = offenderSentence.id,
    createDatetime = createDatetime,
    createUserId = createUserId,
    modifyUserId = modifyUserId,
    modifyDatetime = modifyDatetime,
    auditModule = auditModule,
  )
}
