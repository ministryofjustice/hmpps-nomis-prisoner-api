@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId

@Repository
interface OffenderCourtMovementOutRepository : JpaRepository<OffenderCourtMovementOut, OffenderExternalMovementId> {
  fun findAllByOffenderBooking_BookingId(bookingId: Long): List<OffenderCourtMovementOut>
}
