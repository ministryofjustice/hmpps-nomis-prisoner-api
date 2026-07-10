@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut

@Repository
interface OffenderTapScheduleOutRepository : JpaRepository<OffenderTapScheduleOut, Long> {
  fun findByEventIdAndOffenderBooking_Offender_NomsId(eventId: Long, offenderNo: String): OffenderTapScheduleOut?

  fun countByOffenderBooking_Offender_NomsId_AndTapApplicationIsNotNull(offenderNo: String): Long

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(value = "select tso from OffenderTapScheduleOut tso where (tso.eventId = :eventId)")
  fun findByEventIdOrNullForUpdate(eventId: Long): OffenderTapScheduleOut?
}
