@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut

@Repository
interface OffenderTapMovementOutRepository : JpaRepository<OffenderTapMovementOut, OffenderExternalMovementId> {
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderTapMovementOut>
  fun findAllByOffenderBooking_BookingId(bookingId: Long): List<OffenderTapMovementOut>

  fun findById_OffenderBooking_BookingIdAndId_Sequence(bookingId: Long, sequence: Int): OffenderTapMovementOut?
}
