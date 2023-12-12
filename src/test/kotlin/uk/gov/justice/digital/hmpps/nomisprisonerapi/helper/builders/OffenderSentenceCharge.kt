package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceChargeId

@DslMarker
annotation class OffenderSentenceChargeDslMarker

@NomisDataDslMarker
interface OffenderSentenceChargeDsl

@Component
class OffenderSentenceChargeBuilderFactory() {
  fun builder(): OffenderSentenceChargeBuilder {
    return OffenderSentenceChargeBuilder()
  }
}

class OffenderSentenceChargeBuilder() : OffenderSentenceChargeDsl {
  private lateinit var offenderSentenceCharge: OffenderSentenceCharge

  fun build(
    offenderBooking: OffenderBooking,
    offenderCharge: OffenderCharge,
    sentence: OffenderSentence,
  ): OffenderSentenceCharge = OffenderSentenceCharge(
    id = OffenderSentenceChargeId(
      offenderBooking = offenderBooking,
      sequence = sentence.id.sequence,
      offenderChargeId = offenderCharge.id,
    ),
    offenderCharge = offenderCharge,
    offenderSentence = sentence,
  )
    .also { offenderSentenceCharge = it }
}
