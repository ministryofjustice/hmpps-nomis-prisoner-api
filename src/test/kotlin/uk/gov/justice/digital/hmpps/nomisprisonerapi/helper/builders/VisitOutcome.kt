package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository

@DslMarker
annotation class VisitOutcomeDslMarker

@NomisDataDslMarker
interface VisitOutcomeDsl

@Component
class VisitOutcomeBuilderRepository(
  private val visitOutcomeRepository: VisitVisitorRepository,
  private val eventOutcomeRepository: ReferenceCodeRepository<EventOutcome>,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
) {
  fun save(visitor: VisitVisitor): VisitVisitor = visitOutcomeRepository.save(visitor)
  fun lookupEventOutcome(code: String): EventOutcome = eventOutcomeRepository.findByIdOrNull(EventOutcome.pk(code))!!
  fun lookupEventStatus(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
}

@Component
class VisitOutcomeBuilderFactory(
  private val repository: VisitOutcomeBuilderRepository,
) {
  fun builder() = VisitOutcomeBuilderRepositoryBuilder(repository)
}

class VisitOutcomeBuilderRepositoryBuilder(private val repository: VisitOutcomeBuilderRepository) : VisitOutcomeDsl {
  fun build(
    visit: Visit,
    outcomeReasonCode: String,
    eventOutcomeCode: String,
    eventStatusCode: String,
  ): VisitVisitor = VisitVisitor(
    visit = visit,
    person = null,
    groupLeader = false,
    outcomeReasonCode = outcomeReasonCode,
    eventOutcome = repository.lookupEventOutcome(eventOutcomeCode),
    eventStatus = repository.lookupEventStatus(eventStatusCode),
  )
    .let { repository.save(it) }
}
