package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.util.Optional

@Repository
interface OffenderBookingRepository :
  PagingAndSortingRepository<OffenderBooking, Long>,
  JpaSpecificationExecutor<OffenderBooking>,
  CrudRepository<OffenderBooking, Long> {

  fun findByOffenderNomsIdAndActive(nomsId: String, active: Boolean): Optional<OffenderBooking>

  @Query(
    """
    select booking from OffenderBooking booking 
        join Offender offender on booking.offender = offender 
        where offender.nomsId = :nomsId 
        and booking.bookingSequence = 1
        """,
  )
  fun findLatestByOffenderNomsId(@Param("nomsId") nomsId: String): OffenderBooking?

  fun findAllByOffenderNomsId(@Param("nomsId") nomsId: String): List<OffenderBooking>

  fun findOneByOffenderNomsIdAndBookingSequence(nomsId: String, sequence: Int): OffenderBooking?

  @Query(
    """
      select 
       o.OFFENDER_ID_DISPLAY as prisonerid, 
       b.OFFENDER_BOOK_ID    as bookingid
      from OFFENDER_BOOKINGS b join OFFENDERS o on b.offender_id = o.offender_id
      where OFFENDER_BOOK_ID > :bookingId
        and b.BOOKING_SEQ = 1
        and rownum <= :pageSize
      order by OFFENDER_BOOK_ID
    """,
    nativeQuery = true,
  )
  fun findAllLatestIdsFromId(bookingId: Long, pageSize: Int): List<BookingWithIds>

  @Query(
    """
      select * from (
        select 
         o.OFFENDER_ID_DISPLAY as prisonerid, 
         b.OFFENDER_BOOK_ID    as bookingid
        from OFFENDER_BOOKINGS b join OFFENDERS o on b.offender_id = o.offender_id
        where OFFENDER_BOOK_ID > :bookingId
          and b.BOOKING_SEQ = 1
          and b.ACTIVE_FLAG = 'Y'
        order by OFFENDER_BOOK_ID) 
      where rownum <= :pageSize
    """,
    nativeQuery = true,
  )
  fun findAllLatestActiveIdsFromId(bookingId: Long, pageSize: Int): List<BookingWithIds>
}

interface BookingWithIds {
  fun getPrisonerId(): String
  fun getBookingId(): Long
}
