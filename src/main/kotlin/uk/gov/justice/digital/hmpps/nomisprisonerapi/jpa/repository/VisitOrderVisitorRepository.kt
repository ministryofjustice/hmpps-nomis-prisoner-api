package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderVisitor

@Repository
interface VisitOrderVisitorRepository : CrudRepository<VisitOrderVisitor, Long> {
  /**
   * Will do a "select ... for update" so will wait for any locks to clear
   * This means if the NOMIS screen has locked these rows it will hang for 10 seconds and then throw exception
   * and the whole process will be retried until the lock disappears.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000")])
  fun findAllByIdIn(visitorIds: List<Long>): List<VisitOrderVisitor>
}
