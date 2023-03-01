package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrder

@Repository
interface VisitOrderRepository : CrudRepository<VisitOrder, Long> {
  @Query(value = "SELECT VISIT_ORDER_NUMBER.nextval FROM dual d", nativeQuery = true)
  fun getVisitOrderNumber(): Long
}
