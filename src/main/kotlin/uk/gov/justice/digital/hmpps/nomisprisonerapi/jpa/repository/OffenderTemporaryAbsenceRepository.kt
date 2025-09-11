@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence

@Repository
interface OffenderTemporaryAbsenceRepository : JpaRepository<OffenderTemporaryAbsence, OffenderExternalMovementId> {
  fun findAllByOffenderBooking_Offender_NomsIdAndScheduledTemporaryAbsenceIsNull(offenderNo: String): List<OffenderTemporaryAbsence>

  fun findById_OffenderBooking_BookingIdAndId_Sequence(bookingId: Long, sequence: Int): OffenderTemporaryAbsence?
}
