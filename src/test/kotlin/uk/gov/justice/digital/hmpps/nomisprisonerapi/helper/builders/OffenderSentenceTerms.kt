package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTerm
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTermId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceTermType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class OffenderSentenceTermDslMarker

@NomisDataDslMarker
interface OffenderSentenceTermDsl

@Component
class OffenderSentenceTermBuilderFactory(
  private val repository: OffenderSentenceTermBuilderRepository,
) {
  fun builder(): OffenderSentenceTermBuilder = OffenderSentenceTermBuilder(repository)
}

@Component
class OffenderSentenceTermBuilderRepository(
  val sentenceTermTypeRepository: ReferenceCodeRepository<SentenceTermType>,
) {
  fun lookupSentenceTerm(code: String): SentenceTermType = sentenceTermTypeRepository.findByIdOrNull(SentenceTermType.pk(code))!!
}

class OffenderSentenceTermBuilder(
  private val repository: OffenderSentenceTermBuilderRepository,
) : OffenderSentenceTermDsl {
  private lateinit var offenderSentenceTerm: OffenderSentenceTerm

  fun build(
    offenderBooking: OffenderBooking,
    termSequence: Long,
    startDate: LocalDate,
    endDate: LocalDate?,
    years: Int?,
    months: Int?,
    weeks: Int?,
    days: Int?,
    hours: Int?,
    sentenceTermType: String,
    lifeSentenceFlag: Boolean,
    sentence: OffenderSentence,
  ): OffenderSentenceTerm = OffenderSentenceTerm(
    id = OffenderSentenceTermId(
      offenderBooking = offenderBooking,
      sentenceSequence = sentence.id.sequence,
      termSequence = termSequence,
    ),
    sentenceTermType = repository.lookupSentenceTerm(sentenceTermType),
    years = years,
    months = months,
    weeks = weeks,
    days = days,
    hours = hours,
    lifeSentenceFlag = lifeSentenceFlag,
    startDate = startDate,
    endDate = endDate,
    offenderSentence = sentence,
  )
    .also { offenderSentenceTerm = it }
}
