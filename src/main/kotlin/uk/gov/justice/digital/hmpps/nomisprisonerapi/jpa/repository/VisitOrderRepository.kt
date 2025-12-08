package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrder

@Repository
interface VisitOrderRepository : JpaRepository<VisitOrder, Long> {
  /* TODO revert TO_CHAR cast when hibernate/oracle issue fixed:
  https://github.com/hibernate/hibernate-orm/pull/6613/commits/55bc38dede8429ca3d5ce8674d3b3ac97be8c576 which will be released in hibernate 6.2.4
   */
  @Query(value = "SELECT TO_CHAR(VISIT_ORDER_NUMBER.nextval) FROM dual d", nativeQuery = true)
  fun getVisitOrderNumber(): Long
}
