@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication

@Repository
interface OffenderMovementApplicationRepository : JpaRepository<OffenderMovementApplication, Long> {
  @EntityGraph(type = EntityGraphType.FETCH, value = "offender-movement-app")
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderMovementApplication>

  @EntityGraph(type = EntityGraphType.FETCH, value = "offender-movement-app")
  fun findAllByOffenderBooking_BookingId(bookingId: Long): List<OffenderMovementApplication>

  @EntityGraph(type = EntityGraphType.FETCH, value = "application-only")
  fun findByMovementApplicationIdAndOffenderBooking_Offender_NomsId(applicationId: Long, offenderNo: String): OffenderMovementApplication?

  fun countByOffenderBooking_Offender_NomsId(offenderNo: String): Long

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000")])
  fun findByIdOrNullForUpdate(id: Long): OffenderMovementApplication? = findByIdOrNull(id)
}
