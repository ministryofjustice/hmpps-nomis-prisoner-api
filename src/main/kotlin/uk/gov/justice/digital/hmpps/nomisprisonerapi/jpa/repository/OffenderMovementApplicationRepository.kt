package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication

@Repository
interface OffenderMovementApplicationRepository : JpaRepository<OffenderMovementApplication, Long> {
  @Suppress("ktlint:standard:function-naming")
  @EntityGraph(type = EntityGraphType.FETCH, value = "offender-movement-app")
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderMovementApplication>
}
