package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderFixedTermRecall
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderFixedTermRecallRepository
import java.time.LocalDate

@DslMarker
annotation class OffenderFixedTermRecallDslMarker

@NomisDataDslMarker
interface OffenderFixedTermRecallDsl

@Component
class OffenderFixedTermRecallBuilderRepository(
  private val offenderFixedTermRecallRepository: OffenderFixedTermRecallRepository,
) {
  fun save(offenderFixedTermRecall: OffenderFixedTermRecall): OffenderFixedTermRecall = offenderFixedTermRecallRepository.saveAndFlush(offenderFixedTermRecall)
}

@Component
class OffenderFixedTermRecallBuilderFactory(val repository: OffenderFixedTermRecallBuilderRepository) {
  fun builder() = OffenderFixedTermRecallBuilder(repository)
}

class OffenderFixedTermRecallBuilder(
  private val repository: OffenderFixedTermRecallBuilderRepository,
) : OffenderFixedTermRecallDsl {
  fun build(
    booking: OffenderBooking,
    returnToCustodyDate: LocalDate,
    comments: String?,
    recallLength: Long,
    staff: Staff,
  ): OffenderFixedTermRecall = OffenderFixedTermRecall(
    offenderBooking = booking,
    returnToCustodyDate = returnToCustodyDate,
    comments = comments,
    recallLength = recallLength,
    staff = staff,
  )
    .let { repository.save(it) }
}
