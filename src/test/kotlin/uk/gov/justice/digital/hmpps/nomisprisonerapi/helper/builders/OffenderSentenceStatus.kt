package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceStatusRepository

@DslMarker
annotation class OffenderSentenceStatusDslMarker

@NomisDataDslMarker
interface OffenderSentenceStatusDsl

@Component
class OffenderSentenceStatusBuilderFactory(
  private val repository: OffenderSentenceStatusBuilderRepository,
) {
  fun builder(): OffenderSentenceStatusBuilder = OffenderSentenceStatusBuilder(
    repository,
  )
}

@Component
class OffenderSentenceStatusBuilderRepository(
  val repository: OffenderSentenceStatusRepository,
) {
  fun save(offenderSentenceStatus: OffenderSentenceStatus): OffenderSentenceStatus = repository.save(offenderSentenceStatus)
}

class OffenderSentenceStatusBuilder(
  private val repository: OffenderSentenceStatusBuilderRepository,
) : OffenderSentenceStatusDsl {

  fun build(
    sentence: OffenderSentence,
    statusUpdateStaff: Staff,
  ): OffenderSentenceStatus = OffenderSentenceStatus(

    sentence = sentence,
    statusUpdateReason = "1",
    staffId = statusUpdateStaff.id,
    offenderBooking = sentence.id.offenderBooking,
    sequence = sentence.id.sequence,
  )
    .let { repository.save(it) }
}
