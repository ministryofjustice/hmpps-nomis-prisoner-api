@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut

@Repository
interface OffenderTapMovementOutRepository : JpaRepository<OffenderTapMovementOut, OffenderExternalMovementId> {
  // The tapScheduleOut association is annotated with @NotFound(IGNORE) which means Hibernate cannot proxy it and
  // must resolve it eagerly - explicitly LEFT JOIN FETCH-ing it here avoids one extra SELECT per row. toAddress on
  // both this movement and on its linked schedule out are also fetched eagerly (plain @ManyToOne / forced-eager
  // @NotFound respectively), so they need the same treatment.
  @Query(
    "SELECT mo FROM OffenderTapMovementOut mo LEFT JOIN FETCH mo.tapScheduleOut tso LEFT JOIN FETCH tso.toAddress " +
      "LEFT JOIN FETCH mo.toAddress WHERE mo.offenderBooking.offender.nomsId = :offenderNo",
  )
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderTapMovementOut>

  @Query(
    "SELECT mo FROM OffenderTapMovementOut mo LEFT JOIN FETCH mo.tapScheduleOut tso LEFT JOIN FETCH tso.toAddress " +
      "LEFT JOIN FETCH mo.toAddress WHERE mo.offenderBooking.bookingId = :bookingId",
  )
  fun findAllByOffenderBooking_BookingId(bookingId: Long): List<OffenderTapMovementOut>

  fun findById_OffenderBooking_BookingIdAndId_Sequence(bookingId: Long, sequence: Int): OffenderTapMovementOut?
}
