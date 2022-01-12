package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor

@Repository
interface VisitVisitorRepository : CrudRepository<VisitVisitor, Long> {
  fun findByVisitId(visitId: Long): List<VisitVisitor>
}
