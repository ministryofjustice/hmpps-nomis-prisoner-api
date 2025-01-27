package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository

@DslMarker
annotation class VisitVisitorDslMarker

@NomisDataDslMarker
interface VisitVisitorDsl

@Component
class VisitVisitorBuilderRepository(
  private val visitVisitorRepository: VisitVisitorRepository,
) {
  fun save(visitor: VisitVisitor): VisitVisitor = visitVisitorRepository.save(visitor)
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
  ): VisitVisitor = VisitVisitor(
    visit = visit,
    person = person,
    groupLeader = groupLeader,
  )
    .let { repository.save(it) }
}
