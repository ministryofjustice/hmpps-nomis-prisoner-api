package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentencePurpose
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentencePurposeId

@DslMarker
annotation class SentencePurposeDslMarker

@NomisDataDslMarker
interface SentencePurposeDsl

@Component
class SentencePurposeBuilderFactory {
  fun builder(): SentencePurposeBuilder = SentencePurposeBuilder()
}

class SentencePurposeBuilder : SentencePurposeDsl {
  private lateinit var sentencePurpose: SentencePurpose

  fun build(
    courtOrder: CourtOrder,
    orderPartyCode: String,
    purposeCode: String,
  ): SentencePurpose = SentencePurpose(
    id = SentencePurposeId(orderPartyCode = orderPartyCode, purposeCode = purposeCode, orderId = courtOrder.id),
  )
    .also { sentencePurpose = it }
}
