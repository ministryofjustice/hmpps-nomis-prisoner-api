@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn

@Repository
interface OffenderTemporaryAbsenceReturnRepository : JpaRepository<OffenderTemporaryAbsenceReturn, OffenderExternalMovementId> {
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderTemporaryAbsenceReturn>
  fun findAllByOffenderBooking_Offender_NomsIdAndScheduledTemporaryAbsenceReturnIsNull(offenderNo: String): List<OffenderTemporaryAbsenceReturn>

  fun findById_OffenderBooking_BookingIdAndId_Sequence(bookingId: Long, sequence: Int): OffenderTemporaryAbsenceReturn?
}
