package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceCalculationTypeRepository
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
) {
  fun lookupSentenceCalculationType(calculationType: String, category: String): SentenceCalculationType =
    sentenceCalculationTypeRepository.findByIdOrNull(SentenceCalculationTypeId(calculationType, category))!!
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
    offenderBooking: OffenderBooking,
    sequence: Long,
  ): OffenderSentence = OffenderSentence(
    id = SentenceId(offenderBooking = offenderBooking, sequence = sequence),
    status = status,
    startDate = startDate,
    calculationType = repository.lookupSentenceCalculationType(calculationType, category),
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
