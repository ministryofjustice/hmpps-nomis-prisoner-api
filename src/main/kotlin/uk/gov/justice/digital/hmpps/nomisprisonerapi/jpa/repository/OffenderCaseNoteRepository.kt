package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote

@Repository
interface OffenderCaseNoteRepository : JpaRepository<OffenderCaseNote, Long> {
//  @Suppress("ktlint:standard:function-naming")
//  fun findAllByOffenderBooking_BookingId(bookingId: Long): List<OffenderCaseNote>

  @Suppress("ktlint:standard:function-naming")
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderCaseNote>

  @Query(
    """
      select ob.bookingId from OffenderBooking ob 
        where 
          (:fromId is null or ob.bookingId > :fromId) and 
          (:toId is null or ob.bookingId < :toId) and
          (:activeOnly = false or ob.active)
      order by ob.bookingId asc
    """,
    // Should use index OFFENDER_BOOKINGS_X02 on ACTIVE_FLAG, OFFENDER_BOOK_ID and not need any table access
  )
  fun findAllBookingIds(
    fromId: Long?,
    toId: Long?,
    activeOnly: Boolean = true,
    pageable: Pageable,
  ): Page<Long>
}
