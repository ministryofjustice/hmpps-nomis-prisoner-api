package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti

@Repository
interface OffenderMovementApplicationMultiRepository : JpaRepository<OffenderMovementApplicationMulti, Long> {
  fun findByOffenderMovementApplication(offenderMovementApplication: OffenderMovementApplication): List<OffenderMovementApplicationMulti>

  @Suppress("ktlint:standard:function-naming")
  @EntityGraph(type = EntityGraphType.FETCH, value = "multi-only")
  fun findByMovementApplicationMultiIdAndOffenderMovementApplication_OffenderBooking_Offender_NomsId(movementApplicationMultiId: Long, nomsId: String): OffenderMovementApplicationMulti?
}
