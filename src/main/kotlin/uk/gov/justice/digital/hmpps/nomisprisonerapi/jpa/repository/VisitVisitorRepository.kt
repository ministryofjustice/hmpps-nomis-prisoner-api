package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor

@Repository
interface VisitVisitorRepository : CrudRepository<VisitVisitor, Long> {
  fun findByVisitId(visitId: Long): List<VisitVisitor>

  @Query(value = "SELECT EVENT_ID.nextval FROM dual d", nativeQuery = true)
  fun getEventId(): Long
}
