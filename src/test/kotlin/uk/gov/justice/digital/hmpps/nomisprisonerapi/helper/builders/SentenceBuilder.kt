package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import java.time.LocalDate
import java.time.LocalDateTime

class SentenceBuilder(
  var calculationType: String = "ADIMP_ORA",
  var category: String = "2003",
  var startDate: LocalDate = LocalDate.now(),
  var status: String = "I",
  var adjustments: List<SentenceAdjustmentBuilder> = emptyList(),
) {
  fun build(
    offenderBooking: OffenderBooking,
    sequence: Long = 1,
    calculationType: SentenceCalculationType,
  ): OffenderSentence =
    OffenderSentence(
      id = SentenceId(offenderBooking = offenderBooking, sequence = sequence),
      status = status,
      startDate = startDate,
      calculationType = calculationType,
    )

  fun withAdjustment(sentenceAdjustmentBuilder: SentenceAdjustmentBuilder = SentenceAdjustmentBuilder()): SentenceBuilder {
    adjustments = listOf(sentenceAdjustmentBuilder)
    return this
  }

  fun withAdjustments(vararg sentenceAdjustmentBuilders: SentenceAdjustmentBuilder): SentenceBuilder {
    adjustments = arrayOf(*sentenceAdjustmentBuilders).asList()
    return this
  }
}

class SentenceAdjustmentBuilder(
  var adjustmentTypeCode: String = "UR",
  var adjustmentDate: LocalDate = LocalDate.now(),
  var createdDate: LocalDateTime = LocalDateTime.now(),
  var adjustmentNumberOfDays: Long = 10,
  var kayDateAdjustmentId: Long? = null,
) {
  fun build(sentenceAdjustment: SentenceAdjustment, sentence: OffenderSentence): OffenderSentenceAdjustment =
    OffenderSentenceAdjustment(
      offenderBooking = sentence.id.offenderBooking,
      sentenceSequence = sentence.id.sequence,
      sentence = sentence,
      sentenceAdjustment = sentenceAdjustment,
      adjustmentDate = adjustmentDate,
      createdDate = createdDate,
      adjustmentNumberOfDays = adjustmentNumberOfDays,
      offenderKeyDateAdjustmentId = kayDateAdjustmentId,
    )
}
