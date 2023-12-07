package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCategoryType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import java.time.LocalDate
import java.time.LocalDateTime

class LegacySentenceBuilder(
  var calculationType: String = "ADIMP_ORA",
  var category: String = "2003",
  var startDate: LocalDate = LocalDate.now(),
  var status: String = "I",
  var sentenceLevel: String = "IND",
  var adjustments: List<LegacySentenceAdjustmentBuilder> = emptyList(),
) {
  fun build(
    offenderBooking: OffenderBooking,
    sequence: Long = 1,
    calculationType: SentenceCalculationType,
    category: SentenceCategoryType,
  ): OffenderSentence =
    OffenderSentence(
      id = SentenceId(offenderBooking = offenderBooking, sequence = sequence),
      status = status,
      startDate = startDate,
      calculationType = calculationType,
      category = category,
      sentenceLevel = sentenceLevel,
    )

  fun withAdjustment(sentenceAdjustmentBuilder: LegacySentenceAdjustmentBuilder = LegacySentenceAdjustmentBuilder()): LegacySentenceBuilder {
    adjustments = listOf(sentenceAdjustmentBuilder)
    return this
  }

  fun withAdjustments(vararg sentenceAdjustmentBuilders: LegacySentenceAdjustmentBuilder): LegacySentenceBuilder {
    adjustments = arrayOf(*sentenceAdjustmentBuilders).asList()
    return this
  }
}

class LegacySentenceAdjustmentBuilder(
  var adjustmentTypeCode: String = "UR",
  var adjustmentDate: LocalDate = LocalDate.now(),
  var createdDate: LocalDateTime = LocalDateTime.now(),
  var adjustmentNumberOfDays: Long = 10,
  var keyDateAdjustmentId: Long? = null,
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
      offenderKeyDateAdjustmentId = keyDateAdjustmentId,
    )
}
