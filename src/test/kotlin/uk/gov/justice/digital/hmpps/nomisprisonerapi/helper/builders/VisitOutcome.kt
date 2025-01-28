package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository

@DslMarker
annotation class VisitOutcomeDslMarker

@NomisDataDslMarker
interface VisitOutcomeDsl

@Component
class VisitOutcomeBuilderRepository(
  private val visitOutcomeRepository: VisitVisitorRepository,
) {
  fun save(visitor: VisitVisitor): VisitVisitor = visitOutcomeRepository.save(visitor)
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
    outcomeReason: String,
  ): VisitVisitor = VisitVisitor(
    visit = visit,
    person = null,
    groupLeader = false,
    outcomeReasonCode = outcomeReason,
  )
    .let { repository.save(it) }
}
