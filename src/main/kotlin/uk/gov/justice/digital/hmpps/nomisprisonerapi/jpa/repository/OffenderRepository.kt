package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender

@Repository
interface OffenderRepository :
  JpaRepository<Offender, Long>,
  JpaSpecificationExecutor<Offender> {
  fun existsByNomsId(nomsId: String): Boolean

  fun findByNomsId(nomsId: String): List<Offender>

  /**
   * This returns the root offender first followed by all other offender records.
   * The current offender record (linked to the booking with sequence 1) is only returned first if it is also the
   * root offender record.
   **/
  @Query("select o from Offender o left join fetch o.allBookings b WHERE o.nomsId = :nomsId order by b.bookingSequence asc")
  fun findByNomsIdOrderedWithBookings(nomsId: String): List<Offender>

  /** This returns the root offender id or null if the offender doesn't have any bookings */
  @Query("select o.id from Offender o join o.allBookings b WHERE o.nomsId = :nomsId and b.bookingSequence = 1")
  fun findCurrentIdByNomsId(nomsId: String): Long?

  @Query("select o from Offender o WHERE o.nomsId = :nomsId and o.rootOffenderId = o.id")
  fun findRootByNomsId(nomsId: String): Offender?

  @Query(
    value = """
      select o.nomsId as nomsId, ob.bookingId as bookingId, ob.active as active, ob.inOutStatus as inOutStatus 
        from OffenderBooking ob join ob.offender o 
        where ob.bookingSequence = 1
    """,
    countQuery = """
      select count(ob) from OffenderBooking ob where ob.bookingSequence = 1
    """,
  )
  fun findAllWithBookings(pageable: Pageable): Page<PrisonerIds>

  @Query(
    """
        select
            distinct(ota.id.offender.id) as offenderId
        from OffenderTrustAccount ota join ota.id.offender o
        where o.rootOffenderId = o.id
        and ota.currentBalance != 0 or ota.holdBalance != 0
    """,
  )
  fun findAllWithBalances(pageable: Pageable): Page<Long>

  @Query(
    """
        select o.nomsId as nomsId from Offender o where o.rootOffenderId = o.id
    """,
  )
  fun findAllIds(pageable: Pageable): Page<PrisonerId>

  @Query(
    """
      select /*+ INDEX (OFFENDERS OFFENDERS_PK) */
       offender_id_display as prisonerid, 
       offender_id         as offenderid
      from offenders
      where root_offender_id = offender_id
        and offender_id > :offenderId
        and rownum <= :pageSize
      order by offender_id
    """,
    nativeQuery = true,
  )
  fun findAllIdsFromId(offenderId: Long, pageSize: Int): List<PrisonerWithId>
}

fun OffenderRepository.findLatestAliasByNomisId(nomsId: String): Offender? = findByNomsIdOrderedWithBookings(nomsId).firstOrNull()

interface PrisonerIds {
  fun getNomsId(): String
  fun getBookingId(): Long
  fun isActive(): Boolean
  fun getInOutStatus(): String
}
interface PrisonerId {
  fun getNomsId(): String
}
interface PrisonerWithId {
  fun getPrisonerId(): String
  fun getOffenderId(): Long
}
