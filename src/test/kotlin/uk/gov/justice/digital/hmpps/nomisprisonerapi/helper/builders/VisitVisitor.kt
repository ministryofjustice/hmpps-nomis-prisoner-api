package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository

@DslMarker
annotation class VisitVisitorDslMarker

@NomisDataDslMarker
interface VisitVisitorDsl

@Component
class VisitVisitorBuilderRepository(
  private val visitVisitorRepository: VisitVisitorRepository,
  private val eventOutcomeRepository: ReferenceCodeRepository<EventOutcome>,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
) {
  fun save(visitor: VisitVisitor): VisitVisitor = visitVisitorRepository.save(visitor)
  fun lookupEventOutcome(code: String): EventOutcome = eventOutcomeRepository.findByIdOrNull(EventOutcome.pk(code))!!
  fun lookupEventStatus(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
}

@Component
class VisitVisitorBuilderFactory(
  private val repository: VisitVisitorBuilderRepository,
) {
  fun builder() = VisitVisitorBuilderRepositoryBuilder(repository)
}

class VisitVisitorBuilderRepositoryBuilder(private val repository: VisitVisitorBuilderRepository) : VisitVisitorDsl {
  fun build(
    visit: Visit,
    person: Person,
    groupLeader: Boolean,
    assistedVisit: Boolean,
    outcomeReasonCode: String?,
    eventOutcomeCode: String,
    eventStatusCode: String,
    comment: String?,
  ): VisitVisitor = VisitVisitor(
    visit = visit,
    person = person,
    groupLeader = groupLeader,
    assistedVisit = assistedVisit,
    outcomeReasonCode = outcomeReasonCode,
    eventOutcome = repository.lookupEventOutcome(eventOutcomeCode),
    eventStatus = repository.lookupEventStatus(eventStatusCode),
    commentText = comment,
  )
    .let { repository.save(it) }
}
