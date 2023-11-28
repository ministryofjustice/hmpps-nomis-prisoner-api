package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderKeyDateAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceAdjustmentRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderKeyDateAdjustmentDslMarker

@NomisDataDslMarker
interface OffenderKeyDateAdjustmentDsl

@Component
class OffenderKeyDateAdjustmentBuilderFactory(
  private val repository: OffenderKeyDateAdjustmentBuilderRepository,
) {
  fun builder(): OffenderKeyDateAdjustmentBuilder {
    return OffenderKeyDateAdjustmentBuilder(repository)
  }
}

@Component
class OffenderKeyDateAdjustmentBuilderRepository(
  val sentenceAdjustmentRepository: SentenceAdjustmentRepository,
  val offenderKeyDateAdjustmentRepository: OffenderKeyDateAdjustmentRepository,
) {
  fun lookupKeyDateAdjustment(code: String): SentenceAdjustment = sentenceAdjustmentRepository.findByIdOrNull(code)!!
  fun save(adjustment: OffenderKeyDateAdjustment): OffenderKeyDateAdjustment =
    offenderKeyDateAdjustmentRepository.save(adjustment)
}

class OffenderKeyDateAdjustmentBuilder(
  private val repository: OffenderKeyDateAdjustmentBuilderRepository,
) : OffenderKeyDateAdjustmentDsl {
  private lateinit var offenderKeyDateAdjustment: OffenderKeyDateAdjustment

  fun build(
    adjustmentTypeCode: String,
    adjustmentDate: LocalDate,
    createdDate: LocalDateTime,
    adjustmentNumberOfDays: Long,
    active: Boolean,
    offenderBooking: OffenderBooking,
  ): OffenderKeyDateAdjustment = OffenderKeyDateAdjustment(
    offenderBooking = offenderBooking,
    sentenceAdjustment = repository.lookupKeyDateAdjustment(adjustmentTypeCode),
    adjustmentDate = adjustmentDate,
    adjustmentNumberOfDays = adjustmentNumberOfDays,
    createdDate = createdDate,
    active = active,
  )
    .let { repository.save(it) }
    .also { offenderKeyDateAdjustment = it }
}
