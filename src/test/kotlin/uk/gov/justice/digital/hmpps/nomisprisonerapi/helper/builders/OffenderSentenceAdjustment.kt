package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceAdjustmentRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderSentenceAdjustmentDslMarker

@NomisDataDslMarker
interface OffenderSentenceAdjustmentDsl

@Component
class OffenderSentenceAdjustmentBuilderFactory(
  private val repository: OffenderSentenceAdjustmentBuilderRepository,
) {
  fun builder(): OffenderSentenceAdjustmentBuilder {
    return OffenderSentenceAdjustmentBuilder(repository)
  }
}

@Component
class OffenderSentenceAdjustmentBuilderRepository(
  val sentenceAdjustmentRepository: SentenceAdjustmentRepository,
  val offenderSentenceAdjustmentRepository: OffenderSentenceAdjustmentRepository,
) {
  fun lookupSentenceAdjustment(code: String): SentenceAdjustment = sentenceAdjustmentRepository.findByIdOrNull(code)!!
  fun save(adjustment: OffenderSentenceAdjustment): OffenderSentenceAdjustment =
    offenderSentenceAdjustmentRepository.save(adjustment)
}

class OffenderSentenceAdjustmentBuilder(
  private val repository: OffenderSentenceAdjustmentBuilderRepository,
) : OffenderSentenceAdjustmentDsl {
  private lateinit var offenderSentenceAdjustment: OffenderSentenceAdjustment

  fun build(
    adjustmentTypeCode: String,
    adjustmentDate: LocalDate,
    createdDate: LocalDateTime,
    adjustmentNumberOfDays: Long,
    keyDateAdjustmentId: Long?,
    active: Boolean,
    sentence: OffenderSentence,
  ): OffenderSentenceAdjustment = OffenderSentenceAdjustment(
    offenderBooking = sentence.id.offenderBooking,
    sentenceSequence = sentence.id.sequence,
    sentence = sentence,
    sentenceAdjustment = repository.lookupSentenceAdjustment(adjustmentTypeCode),
    adjustmentDate = adjustmentDate,
    createdDate = createdDate,
    adjustmentNumberOfDays = adjustmentNumberOfDays,
    offenderKeyDateAdjustmentId = keyDateAdjustmentId,
    active = active,
  )
    .let { repository.save(it) }
    .also { offenderSentenceAdjustment = it }
}
