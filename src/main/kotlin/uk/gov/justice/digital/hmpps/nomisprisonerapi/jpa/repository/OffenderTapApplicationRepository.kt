@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication

@Repository
interface OffenderTapApplicationRepository : JpaRepository<OffenderTapApplication, Long> {
  @EntityGraph(type = EntityGraphType.FETCH, value = "tap-application")
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderTapApplication>

  @EntityGraph(type = EntityGraphType.FETCH, value = "tap-application")
  fun findAllByOffenderBooking_BookingId(bookingId: Long): List<OffenderTapApplication>

  @EntityGraph(type = EntityGraphType.FETCH, value = "tap-application-only")
  fun findByTapApplicationIdAndOffenderBooking_Offender_NomsId(applicationId: Long, offenderNo: String): OffenderTapApplication?

  fun countByOffenderBooking_Offender_NomsId(offenderNo: String): Long

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000")])
  @Query("SELECT ota FROM OffenderTapApplication ota WHERE ota.tapApplicationId = :id")
  fun findByIdOrNullForUpdate(id: Long): OffenderTapApplication?
}
