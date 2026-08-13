@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn

@Repository
interface OffenderTapMovementInRepository : JpaRepository<OffenderTapMovementIn, OffenderExternalMovementId> {
  // The tapScheduleIn and tapScheduleOut associations are annotated with @NotFound(IGNORE) which means Hibernate
  // cannot proxy them and must resolve them eagerly - explicitly LEFT JOIN FETCH-ing them here avoids two extra
  // SELECTs per row. fromAddress on this movement, and toAddress on both the schedule out and its own linked
  // movement out, are also fetched eagerly (plain @ManyToOne / forced-eager @NotFound respectively), so they need
  // the same treatment - the mapping code walks all of these to build the tap's "from" address.
  @Query(
    "SELECT mi FROM OffenderTapMovementIn mi LEFT JOIN FETCH mi.tapScheduleIn LEFT JOIN FETCH mi.tapScheduleOut tso " +
      "LEFT JOIN FETCH tso.toAddress LEFT JOIN FETCH tso.tapMovementOut tmo LEFT JOIN FETCH tmo.toAddress " +
      "LEFT JOIN FETCH mi.fromAddress WHERE mi.offenderBooking.offender.nomsId = :offenderNo",
  )
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderTapMovementIn>

  @Query(
    "SELECT mi FROM OffenderTapMovementIn mi LEFT JOIN FETCH mi.tapScheduleIn LEFT JOIN FETCH mi.tapScheduleOut tso " +
      "LEFT JOIN FETCH tso.toAddress LEFT JOIN FETCH tso.tapMovementOut tmo LEFT JOIN FETCH tmo.toAddress " +
      "LEFT JOIN FETCH mi.fromAddress WHERE mi.offenderBooking.bookingId = :bookingId",
  )
  fun findAllByOffenderBooking_BookingId(bookingId: Long): List<OffenderTapMovementIn>

  fun findAllByOffenderBooking_Offender_NomsIdAndTapScheduleInIsNull(offenderNo: String): List<OffenderTapMovementIn>

  fun findById_OffenderBooking_BookingIdAndId_Sequence(bookingId: Long, sequence: Int): OffenderTapMovementIn?
}
